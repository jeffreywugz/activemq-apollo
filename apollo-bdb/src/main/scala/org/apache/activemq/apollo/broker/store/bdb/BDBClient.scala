/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.apollo.broker.store.bdb

import dto.BDBStoreDTO
import java.{lang=>jl}
import java.{util=>ju}

import java.util.concurrent.atomic.AtomicInteger
import collection.mutable.ListBuffer
import org.apache.activemq.apollo.broker.store._
import org.apache.activemq.apollo.util._
import com.sleepycat.je._
import java.io.{EOFException, InputStream, OutputStream}
import org.fusesource.hawtbuf.proto.{MessageBuffer, PBMessageFactory}

object BDBClient extends Log
/**
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class BDBClient(store: BDBStore) extends DispatchLogging {

  import HelperTrait._

  override def log: Log = BDBClient

  def dispatchQueue = store.dispatch_queue

  /////////////////////////////////////////////////////////////////////
  //
  // Helpers
  //
  /////////////////////////////////////////////////////////////////////

  private def directory = config.directory

  /////////////////////////////////////////////////////////////////////
  //
  // Public interface used by the BDBStore
  //
  /////////////////////////////////////////////////////////////////////

  var config: BDBStoreDTO = null

  var environment:Environment = _

  var zero_copy_buffer_allocator: FileZeroCopyBufferAllocator = _

  def zero_copy_dir = {
    import FileSupport._
    config.directory / "zerocp"
  }

  def start() = {
    val env_config = new EnvironmentConfig();
    env_config.setAllowCreate(true);
    env_config.setTransactional(true);
    env_config.setTxnSerializableIsolation(false)

    directory.mkdirs

    if( Option(config.zero_copy).map(_.booleanValue).getOrElse(false) ) {
      zero_copy_buffer_allocator = new FileZeroCopyBufferAllocator(zero_copy_dir)
      zero_copy_buffer_allocator.start
    }

    environment = new Environment(directory, env_config);

    with_ctx { ctx=>
      import ctx._
      messages_db
      message_refs_db
      queues_db

      if( zero_copy_buffer_allocator!=null ) {
        zerocp_db.cursor(tx) { (_,value)=>
          val v = encode_zcp_value(value)
          zero_copy_buffer_allocator.alloc_at(v._1, v._2, v._3)
          true
        }
      }
    }
  }

  def stop() = {
    environment.close
    if( zero_copy_buffer_allocator!=null ) {
      zero_copy_buffer_allocator.stop
      zero_copy_buffer_allocator = null
    }
  }

  case class TxContext(tx:Transaction) {

    def with_entries_db[T](queue_key:Long)(func: (Database) => T): T = {
      val db = environment.openDatabase(tx, entries_db_name(queue_key), long_key_conf)
      try {
        func(db)
      } finally {
        db.close
      }
    }


    private var _messages_db:Database = _
    def messages_db:Database = {
      if( _messages_db==null ) {
        _messages_db = environment.openDatabase(tx, "messages", long_key_conf)
      }
      _messages_db
    }

    private var _zerocp_db:Database = _
    def zerocp_db:Database = {
      if( _zerocp_db==null ) {
        _zerocp_db = environment.openDatabase(tx, "zerocp", long_key_conf)
      }
      _zerocp_db
    }

    private var _message_refs_db:Database = _
    def message_refs_db:Database = {
      if( _message_refs_db==null ) {
        _message_refs_db = environment.openDatabase(tx, "message_refs", long_key_conf)
      }
      _message_refs_db
    }

    private var _queues_db:Database = _
    def queues_db:Database = {
      if( _queues_db==null ) {
        _queues_db = environment.openDatabase(tx, "queues", long_key_conf)
      }
      _queues_db
    }

    def close(ok:Boolean) = {
      if( _messages_db!=null ) {
        _messages_db.close
      }
      if( _message_refs_db!=null ) {
        _message_refs_db.close
      }
      if( _queues_db!=null ) {
        _queues_db.close
      }

      if(ok){
        tx.commit
      } else {
        tx.abort
      }
    }

  }


  def with_ctx[T](func: (TxContext) => T): T = {
    val ctx = TxContext(environment.beginTransaction(null, null));
    var ok = false
    try {
      val rc = func(ctx)
      ok = true
      rc
    } finally {
      ctx.close(ok)
    }
  }


  def purge() = {

    with_ctx { ctx=>
      import ctx._


      def remove_db(name: String): Unit = {
        try {
          environment.removeDatabase(tx, name)
        } catch {
          case x: DatabaseNotFoundException =>
        }
      }

      listQueues.foreach{ queue_key=>
        val name = entries_db_name(queue_key)
        remove_db(name)
      }
      remove_db("messages")
      remove_db("message_refs")
      remove_db("queues")

      messages_db
      message_refs_db
      queues_db
    }

  }

  def add_and_get[K](db:Database, key:DatabaseEntry, amount:Int, tx:Transaction):Int = {
    db.get(tx, key) match {
      case None =>
        if( amount!=0 ) {
          db.put(tx, key, amount)
        }
        amount

      case Some(value) =>
        val counter:Int = value
        val update = counter + amount
        if( update == 0 ) {
          db.delete(tx, key)
        } else {
          db.put(tx, key, update)
        }
        update
    }
  }

  def addQueue(record: QueueRecord, callback:Runnable) = {
    with_ctx { ctx=>
      import ctx._
      queues_db.put(tx, record.key, record)
      with_entries_db(record.key) { entriesdb=> 
      }
    }
    callback.run
  }

  def decrement_message_reference(ctx:TxContext, msg_key:Long) = {
    import ctx._
    if( add_and_get(message_refs_db, msg_key, -1, tx)==0 ) {
      messages_db.delete(tx, msg_key)
      if( zero_copy_buffer_allocator!=null ){
        zerocp_db.get(tx, to_database_entry(msg_key)).foreach { v=>
          val location  = decode_zcp_value(v)
          zero_copy_buffer_allocator.free(location._1, location._2, location._3)
        }
        zerocp_db.delete(tx, msg_key)
      }
    }
  }

  def removeQueue(queue_key: Long, callback:Runnable) = {
    with_ctx { ctx=>
      import ctx._

      queues_db.delete(tx, queue_key)
      with_entries_db(queue_key) { entries_db=>

        entries_db.cursor(tx) { (key,value)=>
          val queueEntry:QueueEntryRecord = value
          decrement_message_reference(ctx, queueEntry.message_key)
          true // keep cursoring..
        }

      }

      environment.removeDatabase(tx, entries_db_name(queue_key))
    }
    callback.run
  }

  def store(uows: Seq[BDBStore#DelayableUOW], callback:Runnable) {
    with_ctx { ctx=>
      import ctx._
      var zcp_files_to_sync = Set[Int]()
      uows.foreach { uow =>
          uow.actions.foreach {
            case (msg, action) =>

              val message_record = action.messageRecord
              if (message_record != null) {
                import PBSupport._

                val pb = if( message_record.zero_copy_buffer != null ) {
                  val r = to_pb(action.messageRecord).copy
                  val buffer = zero_copy_buffer_allocator.to_alloc_buffer(message_record.zero_copy_buffer)
                  r.setZcpFile(buffer.file)
                  r.setZcpOffset(buffer.offset)
                  r.setZcpSize(buffer.size)
                  zerocp_db.put(tx, message_record.key, (buffer.file, buffer.offset, buffer.size))
                  zcp_files_to_sync += buffer.file
                  r.freeze
                } else {
                  to_pb(action.messageRecord)
                }

                messages_db.put(tx, action.messageRecord.key, pb)
              }

              action.enqueues.foreach { queueEntry =>
                with_entries_db(queueEntry.queue_key) { entries_db=>
                  entries_db.put(tx, queueEntry.entry_seq, queueEntry)
                  add_and_get(message_refs_db, queueEntry.message_key, 1, tx)
                }
              }

              action.dequeues.foreach { queueEntry =>
                with_entries_db(queueEntry.queue_key) { entries_db=>
                  entries_db.delete(tx, queueEntry.entry_seq)
                  decrement_message_reference(ctx, queueEntry.message_key)
                }
              }
          }
      }
      if( zero_copy_buffer_allocator!=null ) {
        zcp_files_to_sync.foreach(zero_copy_buffer_allocator.sync(_))
      }
    }
    callback.run
  }

  def listQueues: Seq[Long] = {
    val rc = ListBuffer[Long]()
    with_ctx { ctx=>
      import ctx._

      queues_db.cursor(tx) { (key, _) =>
        rc += key
        true // to continue cursoring.
      }
    }

    rc
  }

  def getQueue(queue_key: Long): Option[QueueRecord] = {
    with_ctx { ctx=>
      import ctx._
      queues_db.get(tx, to_database_entry(queue_key)).map( x=> to_queue_record(x)  )
    }
  }

  def listQueueEntryGroups(queue_key: Long, limit: Int) : Seq[QueueEntryRange] = {
    var rc = ListBuffer[QueueEntryRange]()
    with_ctx { ctx=>
      import ctx._

      with_entries_db(queue_key) { entries_db=>

        var group:QueueEntryRange = null

        entries_db.cursor(tx) { (key, value) =>

          if( group == null ) {
            group = new QueueEntryRange
            group.first_entry_seq = key
          }

          val entry:QueueEntryRecord = value

          group.last_entry_seq = key
          group.count += 1
          group.size += entry.size

          if( group.count == limit) {
            rc += group
            group = null
          }

          true // to continue cursoring.
        }

        if( group!=null ) {
          rc += group
        }

      }
    }
    rc
  }

  def getQueueEntries(queue_key: Long, firstSeq:Long, lastSeq:Long): Seq[QueueEntryRecord] = {
    var rc = ListBuffer[QueueEntryRecord]()
    with_ctx { ctx=>
      import ctx._

      with_entries_db(queue_key) { entries_db=>
        entries_db.cursor_from(tx, to_database_entry(firstSeq)) { (key, value) =>
          val entry_seq:Long = key
          val entry:QueueEntryRecord = value
          rc += entry
          entry_seq < lastSeq
        }
      }
    }
    rc
  }

  val metric_load_from_index_counter = new TimeCounter
  var metric_load_from_index = metric_load_from_index_counter(false)

  def loadMessages(requests: ListBuffer[(Long, (Option[MessageRecord])=>Unit)]) = {

    with_ctx { ctx=>
      import ctx._
      requests.foreach { case (message_key, callback)=>
        val record = metric_load_from_index_counter.time {
          messages_db.get(tx, to_database_entry(message_key)).map{ data=>
            import PBSupport._
            val pb:MessagePB.Buffer = data
            val rc = from_pb(pb)
            if( pb.hasZcpFile ) {
              rc.zero_copy_buffer = zero_copy_buffer_allocator.view_buffer(pb.getZcpFile, pb.getZcpOffset, pb.getZcpSize)
            }
            rc
          }
        }
        callback(record)
      }
    }
  }


  def getLastMessageKey:Long = {
    with_ctx { ctx=>
      import ctx._

      messages_db.last_key(tx).map(to_long _).getOrElse(0)
    }
  }

  def getLastQueueKey:Long = {
    with_ctx { ctx=>
      import ctx._

      queues_db.last_key(tx).map(to_long _).getOrElse(0)
    }
  }

  def export_pb(streams:StreamManager[OutputStream]):Result[Zilch,String] = {
    try {
      with_ctx { ctx=>
        import ctx._
        import PBSupport._

        streams.using_queue_stream { queue_stream =>
          queues_db.cursor(tx) { (_, value) =>
            val record:QueueRecord = value
            record.writeFramed(queue_stream)
            true
          }
        }

        streams.using_message_stream { message_stream=>
          messages_db.cursor(tx) { (_, data) =>
            import PBSupport._
            val pb = MessagePB.FACTORY.parseUnframed(data.getData)
            if( pb.hasZcpFile ) {
              val zcpb = zero_copy_buffer_allocator.view_buffer(pb.getZcpFile, pb.getZcpOffset, pb.getZcpSize)
              var data = pb.copy
              data.clearZcpFile
              data.clearZcpFile
              // write the pb frame and then the direct buffer data..
              data.freeze.writeFramed(message_stream)
              zcpb.read(message_stream)
            } else {
              pb.writeFramed(message_stream)
            }
            true
          }
        }

        streams.using_queue_entry_stream { queue_entry_stream=>
          queues_db.cursor(tx) { (_, value) =>
            val record:QueueRecord = value
            with_entries_db(record.key) { entries_db=>
              entries_db.cursor(tx) { (key, value) =>
                val record:QueueEntryRecord = value
                record.writeFramed(queue_entry_stream)
                true
              }
            }
            true
          }
        }

      }
      Success(Zilch)
    } catch {
      case x:Exception=>
        Failure(x.getMessage)
    }
  }

  def import_pb(streams:StreamManager[InputStream]):Result[Zilch,String] = {
    try {
      purge

      def foreach[Buffer] (stream:InputStream, fact:PBMessageFactory[_,_])(func: (Buffer)=>Unit):Unit = {
        var done = false
        do {
          try {
            func(fact.parseFramed(stream).asInstanceOf[Buffer])
          } catch {
            case x:EOFException =>
              done = true
          }
        } while( !done )
      }

      with_ctx { ctx=>
        import ctx._
        import PBSupport._

        streams.using_queue_stream { queue_stream=>
          foreach[QueuePB.Buffer](queue_stream, QueuePB.FACTORY) { pb=>
            val record:QueueRecord = pb
            queues_db.put(tx, record.key, record)
            with_entries_db(record.key) { entriesdb=>
            }
          }
        }

        var zcp_counter = 0
        val max_ctx = zero_copy_buffer_allocator.contexts.size

        streams.using_message_stream { message_stream=>
          foreach[MessagePB.Buffer](message_stream, MessagePB.FACTORY) { pb=>

            val record:MessagePB.Buffer = if( pb.hasZcpSize ) {
              val cp = pb.copy
              val zcpb = zero_copy_buffer_allocator.contexts(zcp_counter % max_ctx).alloc(cp.getZcpSize)
              cp.setZcpFile(zcpb.file)
              cp.setZcpOffset(zcpb.offset)

              zcp_counter += 1
              zcpb.write(message_stream)

              zerocp_db.put(tx, pb.getMessageKey, (zcpb.file, zcpb.offset, zcpb.size))
              cp.freeze
            } else {
              pb
            }

            messages_db.put(tx, record.getMessageKey, record)
          }
        }

        streams.using_queue_entry_stream { queue_entry_stream=>
          foreach[QueueEntryPB.Buffer](queue_entry_stream, QueueEntryPB.FACTORY) { pb=>
            val record:QueueEntryRecord = pb

            with_entries_db(record.queue_key) { entries_db=>
              entries_db.put(tx, record.entry_seq, record)
              add_and_get(message_refs_db, record.message_key, 1, tx)
            }
          }
        }
      }
      Success(Zilch)

    } catch {
      case x:Exception=>
        Failure(x.getMessage)
    }
  }
}
