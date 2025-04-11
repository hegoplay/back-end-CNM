package iuh.fit.se.model.converter;

import iuh.fit.se.model.ConversationType;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
public class TypeConversationConverter implements AttributeConverter<ConversationType> {

    @Override
    public AttributeValue transformFrom(ConversationType input) {
        return AttributeValue.builder()
                .s(input.getValue())
                .build();
    }

    @Override
    public ConversationType transformTo(AttributeValue attributeValue) {
        return ConversationType.fromValue(attributeValue.s());
    }

    @Override
    public EnhancedType<ConversationType> type() {
        return EnhancedType.of(ConversationType.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S; // Lưu dưới dạng chuỗi trong DynamoDB
    }
}