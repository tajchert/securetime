package pl.tajchert.securetime

import pl.tajchert.securetime.protocol.RtMessage
import pl.tajchert.securetime.protocol.RtWire
import pl.tajchert.securetime.util.BytesUtil
import timber.log.Timber
import java.math.BigInteger
import java.net.InetSocketAddress
import java.net.StandardProtocolFamily
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.time.Instant
import java.util.*

public class SecureTime(
    val host: String,
    val port: Int,
    serverPubKeyHexdecimal: String? = null,
    serverPubKeyBase64: String? = null
) {

    private val client = RoughtimeClient(
        //We can provide Base64 or Hex value of publicKey
        if (serverPubKeyHexdecimal != null) {
            BytesUtil.hexToBytes(
                serverPubKeyHexdecimal
            )
        } else {
            BytesUtil.hexToBytes(
                convertBase64ToHex(serverPubKeyBase64!!)
            )
        }
    )

    fun getTime(): Triple<Instant, Int, Long> {
        val addr = InetSocketAddress(host, port)

        val channel = DatagramChannel.open(StandardProtocolFamily.INET)
        channel.configureBlocking(false)

        // Create a request message
        val request = client.createRequest()

        // Encode for transmission
        val encodedRequest = RtWire.toWire(request)

        // Send the message
        channel.send(encodedRequest.nioBuffer(), addr)
        val bytesWritten = channel.send(encodedRequest.nioBuffer(), addr)

        // Ensure the message was sent
        if (bytesWritten != encodedRequest.readableBytes()) {
            throw RuntimeException("failed to fully write request")
        }

        // Space for receiving the reply
        val recvBuf = ByteBuffer.allocate(4096)
        var attempts = 50

        // Simple loop to look for the first response. Wait for max 5 seconds.
        while (--attempts > 0) {
            recvBuf.clear()
            channel.receive(recvBuf)
            recvBuf.flip()
            if (recvBuf.hasRemaining()) {
                break
            }
            Thread.sleep(100L)
        }
        val timeLocalReceived = Instant.now()

        if (recvBuf.hasRemaining()) {
            // A reply from the server has been received
            System.out.printf("Read message of %d bytes from %s:\n", recvBuf.remaining(), addr)

            // Parse the response
            val response = RtMessage.fromByteBuffer(recvBuf)
            System.out.printf(response.toString())

            // Validate the response. Checks that the message is well-formed, all signatures are valid,
            // and our nonce is present in the response.
            client.processResponse(response)

            if (client.isResponseValid) {
                // Validation passed, the response is good

                // The "midpoint" is the Roughtime server's reported timestamp (in microseconds). And the
                // "radius" is a span of uncertainty around that midpoint. A Roughtime server asserts that
                // its "true time" lies within the span.
                val midpoint = Instant.ofEpochMilli(client.midpoint() / 1_000L)
                val radiusMiliseconds = client.radius() / 1_000
                System.out.printf("midpoint    : $midpoint (radius $radiusMiliseconds sec)")

                val localTimeDiffMiliseconds = Math.abs(timeLocalReceived.toEpochMilli() - midpoint.toEpochMilli())
                return Triple(midpoint, radiusMiliseconds, localTimeDiffMiliseconds)
            } else {
                // Validation failed. Print out the reason why.
                Timber.e(client.invalidResponseCause().message, "Response INVALID ")
                throw RuntimeException("Validation failed, invalid server response: " + client.invalidResponseCause().message)
            }
        } else {
            // No reply within 5 seconds
            Timber.e("No response from $addr during 5 sec waiting")
            throw RuntimeException("No response during 5 seconds")
        }
    }

    private fun convertBase64ToHex(valueBase64: String): String {
        val decoded = Base64.getDecoder().decode(valueBase64)
        return String.format("%040x", BigInteger(1, decoded))
    }
}