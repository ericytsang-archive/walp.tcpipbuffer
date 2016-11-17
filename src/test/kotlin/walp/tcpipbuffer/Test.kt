package walp.tcpipbuffer

import org.junit.After
import org.junit.Test
import java.io.DataInputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ArrayBlockingQueue
import kotlin.concurrent.thread

/**
 * tests if the receiving side of a TCP IP connection will buffer data sent to
 * it by the sending side without propagating it up to the application level.
 */
class Test
{
    companion object
    {
        private const val PORT = 50629
    }

    val sock1:Socket
    val sock2:Socket

    init
    {
        // create the connection
        val q = ArrayBlockingQueue<Socket>(1)
        thread {q.put(ServerSocket(PORT).use {it.accept()})}
        sock1 = Socket("localhost",PORT)
        sock2 = q.take()

        // exchange a lot of data to make the windows larger
        run {
            val t1 = thread {DataInputStream(sock2.inputStream).readFully(ByteArray(2048,{0}))}
            sock1.outputStream.write(ByteArray(2048,{0}))
            t1.join()
        }
        run {
            val t1 = thread {DataInputStream(sock1.inputStream).readFully(ByteArray(2048,{0}))}
            sock2.outputStream.write(ByteArray(2048,{0}))
            t1.join()
        }
    }

    @After
    fun teardown()
    {
        try {sock1.close()} catch (ex:Exception) {}
        try {sock2.close()} catch (ex:Exception) {}
    }

    @Test
    fun sendWithoutReceiving()
    {
        sock1.outputStream.write(5)
    }

    @Test
    fun sendAndCloseWithoutReceiving()
    {
        sock1.outputStream.write(5)
        sock1.close()
    }

    @Test
    fun sendAndCloseThenReceive()
    {
        sock1.outputStream.write(5)
        sock1.close()
        assert(sock2.inputStream.read() == 5)
    }

    @Test
    fun sendAndCloseThenReceiveMoreThanSent()
    {
        sock1.outputStream.write(5)
        sock1.close()
        assert(sock2.inputStream.read() == 5)
        assert(sock2.inputStream.read() == -1)
    }
}
