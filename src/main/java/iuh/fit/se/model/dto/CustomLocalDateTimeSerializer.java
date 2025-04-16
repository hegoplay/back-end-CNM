package iuh.fit.se.model.dto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CustomLocalDateTimeSerializer extends StdSerializer<LocalDateTime> {
    private static final long serialVersionUID = -798980219387045759L;
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public CustomLocalDateTimeSerializer() {
        this(null);
    }

    public CustomLocalDateTimeSerializer(Class<LocalDateTime> t) {
        super(t);
    }

    @Override
    public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        // Lấy ngày hiện tại
        LocalDateTime now = LocalDateTime.now();
        // Kiểm tra xem updatedAt có thuộc ngày hiện tại không
        if (value.toLocalDate().isEqual(now.toLocalDate())) {
            // Trong ngày: trả về giờ và phút
            gen.writeString(value.format(TIME_FORMATTER));
        } else {
            // Qua ngày: trả về ngày, tháng, năm
            gen.writeString(value.format(DATE_FORMATTER));
        }
    }
}