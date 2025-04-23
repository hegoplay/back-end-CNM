package iuh.fit.se.config;

import com.corundumstudio.socketio.AckCallback;
import com.corundumstudio.socketio.protocol.AckArgs;
import com.corundumstudio.socketio.protocol.JsonSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class CustomJsonSupport implements JsonSupport {
    private final ObjectMapper objectMapper;

    public CustomJsonSupport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> T readValue(String eventName, ByteBufInputStream src, Class<T> valueType) throws IOException {
        try {
            JsonNode node = objectMapper.readTree(src);
            if (node.isArray() && node.size() >= 2) {
                // Extract payload (second element in the array)
                return objectMapper.convertValue(node.get(1), valueType);
            }
            // Fallback for other cases
            return objectMapper.convertValue(node, valueType);
        } catch (Exception e) {
            throw new IOException("Error parsing Socket.IO message for event " + eventName, e);
        }
    }


    @Override
    public void writeValue(ByteBufOutputStream out, Object value) throws IOException {
        objectMapper.writeValue((OutputStream)out, value);
    }

	@Override
	public AckArgs readAckArgs(ByteBufInputStream src, AckCallback<?> callback)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addEventMapping(String namespaceName, String eventName,
			Class<?>... eventClass) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeEventMapping(String namespaceName, String eventName) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<byte[]> getArrays() {
		// TODO Auto-generated method stub
		return null;
	}

}