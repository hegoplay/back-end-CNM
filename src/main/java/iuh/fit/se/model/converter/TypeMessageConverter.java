package iuh.fit.se.model.converter;


import iuh.fit.se.model.enumObj.ConversationType;
import iuh.fit.se.model.enumObj.MessageType;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
public class TypeMessageConverter implements AttributeConverter<MessageType> {

    @Override
    public AttributeValue transformFrom(MessageType input) {
        return AttributeValue.builder()
                .s(input.getValue())
                .build();
    }

    @Override
    public MessageType transformTo(AttributeValue attributeValue) {
        return MessageType.fromValue(attributeValue.s());
    }

    @Override
    public EnhancedType<MessageType> type() {
        return EnhancedType.of(MessageType.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S; // Lưu dưới dạng chuỗi trong DynamoDB
    }
}