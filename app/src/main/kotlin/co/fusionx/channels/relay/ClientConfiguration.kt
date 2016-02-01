package co.fusionx.channels.relay

import co.fusionx.relay.ConnectionConfiguration

public class ClientConfiguration(public val name: String,
                                 public val connectionConfiguration: ConnectionConfiguration) {
    override fun equals(other: Any?): Boolean {
        if (other !is ClientConfiguration) return false
        return other.name == name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}