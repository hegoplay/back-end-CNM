package iuh.fit.se.serviceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import iuh.fit.se.mapper.ConversationMapper;
import iuh.fit.se.mapper.MessageMapper;
import iuh.fit.se.model.Conversation;
import iuh.fit.se.model.ConversationMember;
import iuh.fit.se.model.Message;
import iuh.fit.se.model.User;
import iuh.fit.se.model.dto.UserResponseDto;
import iuh.fit.se.model.dto.conversation.ConversationDetailDto;
import iuh.fit.se.model.dto.conversation.ConversationDto;
import iuh.fit.se.model.dto.conversation.CreateGroupRequest;
import iuh.fit.se.model.dto.conversation.MemberDto;
import iuh.fit.se.model.dto.message.MessageResponseDTO;
import iuh.fit.se.model.enumObj.ConversationType;
import iuh.fit.se.repo.ConversationMemberRepository;
import iuh.fit.se.repo.ConversationRepository;
import iuh.fit.se.repo.MessageRepository;
import iuh.fit.se.repo.UserRepository;
import iuh.fit.se.service.ConversationService;

import iuh.fit.se.service.MessageNotifier;
import iuh.fit.se.service.UserService;
import iuh.fit.se.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationServiceAWSImpl implements ConversationService {

	@Value("${aws.region}")
	private String region;
	private final ConversationRepository conversationRepository;
	private final MessageRepository messageRepository;
	private final UserRepository userRepository;
	private final DynamoDbTable<User> userTable;
	private final DynamoDbTable<Conversation> conversationTable;
	private final DynamoDbEnhancedClient enhancedClient;
	private final MessageNotifier messageNotifier;
	private final MessageMapper messageMapper;
	private final ConversationMapper conversationMapper;
	private final AwsService awsService;
	private final UserService userService;
	private final ConversationMemberRepository conversationMemberRepository;
	
	@Override
	public void createFriendConversation(String userPhone, String friendPhone) {
		// TODO Auto-generated method stub
		log.info("Start creating conversation");
		User user = userRepository.findByPhone(userPhone);
		if (user == null) {
			log.warn("User with id {} not found", userPhone);
			return;
		}
		User friend = userRepository.findByPhone(friendPhone);
		if (friend == null) {
			log.warn("User with id {} not found", friendPhone);
			return;
		}
		
		String conversationId = userPhone + "_" + friendPhone;
		
		if(user.getConversations().contains(conversationId)) {
			log.warn("Conversation already exists");
			return;
		}
		if(friend.getConversations().contains(conversationId)) {
			log.warn("Conversation already exists");
			return;
		}
		
		if (user.getConversations() == null) {
			user.setConversations(new ArrayList<>());
		}
		if (friend.getConversations() == null) {
			friend.setConversations(new ArrayList<>());
		}
		
		user.getConversations().add(conversationId);
		friend.getConversations().add(conversationId);
		
		Conversation conversation = Conversation.builder()
						.id(userPhone + "_" + friendPhone)
						.callInProgress(false)
						.type(ConversationType.PRIVATE)
						.participants(List.of(userPhone, friendPhone))
						.createdAt(LocalDateTime.now())
						.updatedAt(LocalDateTime.now())
						.conversationName("ban")
						.conversationImgUrl("xxx")
						.leader(userPhone)
						.admins(List.of(userPhone, friendPhone))
						.messages(List.of())
						.currentCallId(null)
						.build();
		try {
	        enhancedClient.transactWriteItems(request -> request
	            .addPutItem(conversationTable, conversation)
	            .addPutItem(userTable, user)
	            .addPutItem(userTable, friend)
	        );
	        log.info("Transaction completed successfully");
	        ConversationDetailDto conversationDetailDto = getConversationDetail(conversationId);
	        messageNotifier.notifyNewConversation(conversationDetailDto, userPhone);
	        messageNotifier.notifyNewConversation(conversationDetailDto, friendPhone);
	        
	    } catch (TransactionCanceledException e) {
	        log.error("Transaction cancelled: {}", e.cancellationReasons());
	        throw new RuntimeException("Transaction failed", e);
	    }
	}
/**
 * cái phone này là của người dùng đang đăng nhập
 */
	@Override
	public List<ConversationDto> getConversations(String phone) {
		// TODO Auto-generated method stub
		try {
//			List<Conversation> conversations = conversationRepository.findByUserId(userId);
			User user = userRepository.findByPhone(phone);
			if (user == null) {
				log.warn("User with id {} not found", phone);
				return List.of();
			}
			List<Conversation> conversations = new ArrayList<>();
//			for (String conversationId : user.getConversations()) {
//				Conversation conversation = conversationRepository.findById(conversationId);
//				if (conversation != null) {
//					conversations.add(conversation);
//				}
//			}
	        if (user.getConversations() != null) {
	            for (String conversationId : user.getConversations()) {
	                Conversation conversation = conversationRepository.findById(conversationId);
	                if (conversation != null) {
	                    conversations.add(conversation);
	                }
	            }
	        }
			conversations.sort((c1, c2) -> c2.getUpdatedAt().compareTo(c1.getUpdatedAt()));
			List<ConversationDto> conversationDtos = new ArrayList<>();
			
			for (Conversation conversation : conversations) {
				ConversationDto conversationDto = conversationMapper.fromConversationToDto(conversation);
//		Thêm tin nhắn cuối vào conversationDto		
				appendLastMessageIntoConversationDto(conversationDto);
				String conversationId = conversation.getId();
//				cài đặt lại conversation name và conversation img url
				if (conversation.getType() == ConversationType.PRIVATE) {
					String[] split = conversationId.split("_");
					String otherUserId = split[0].equals(phone) ? split[1] : split[0];
					User otherUser = userRepository.findByPhone(otherUserId);
					if (otherUser != null) {
						conversationDto.setConversationName(otherUser.getName());
						conversationDto.setConversationImgUrl(otherUser.getBaseImg());
					} else {
						log.warn("User with id {} not found", otherUserId);
					}
				}
				
				
				conversationDtos.add(conversationDto);
			}
			return conversationDtos;
		} catch (Exception e) {
			log.error("Error getting conversations: {}", e.getMessage());
			throw new RuntimeException("Error getting conversations", e);
		}		
	}

	@Override
	public ConversationDetailDto getConversationDetail(String conversationId) {
		// TODO Auto-generated method stub
		Conversation conversation = conversationRepository.findById(conversationId);
		if (conversation == null) {
//				log.warn("Conversation with id {} not found", conversationId);
			throw new RuntimeException("Conversation not found");
		}
		log.info(conversation.toString());
		ConversationDetailDto conversationDetailDto = conversationMapper.fromConversationToDetailDto(conversation);
		
		log.info("Conversation detail: {}", conversationDetailDto);
		
		List<Message> messagesList = messageRepository.findMessagesByConversationId(conversationId);
		List<MessageResponseDTO> messages = new ArrayList<>();
		
		for (Message message : messagesList) {
			MessageResponseDTO messageResponseDTO = messageMapper.toMessageResponseDto(message);
			messages.add(messageResponseDTO);
		}
		
		
		conversationDetailDto.setMessageDetails((messages));
		
		return conversationDetailDto;
	}

	@Override
	public void updateLastUpdated(String conversationId) {
		// TODO Auto-generated method stub
		Conversation conversation = conversationRepository.findById(conversationId);
		if (conversation == null) {
			log.warn("Conversation with id {} not found", conversationId);
			throw new RuntimeException("Conversation not found");
		}
		conversation.setUpdatedAt(LocalDateTime.now());
		conversationRepository.save(conversation);
	}
	@Override
	public void markAllMessagesAsRead(String conversationId, String userId) {
		// TODO Auto-generated method stub
		
		Conversation conversation = conversationRepository.findById(conversationId);
		if (conversation == null) {
			log.warn("Conversation with id {} not found", conversationId);
			throw new RuntimeException("Conversation not found");
		}
		List<String> messageIds = conversation.getMessages();
		if (messageIds == null || messageIds.isEmpty()) {
			log.warn("No messages found in conversation with id {}", conversationId);
			return;
		}
		for (String messageId : messageIds) {
			Message message = messageRepository.getMessageById(messageId);
			if (message != null && !message.getSeenBy().contains(userId)) {
				message.getSeenBy().add(userId);
				messageRepository.save(message);
			}
		}
		// Notify the user about the read status
		messageNotifier.notifyAllMessagesRead(conversationId, userId);
		log.info("All messages in conversation {} marked as read by user {}", conversationId, userId);
	}
	@Override
	public void deleteFriendConversation(String userId, String friendId) {
		// TODO Auto-generated method stub
		String conversationId = userId + "_" + friendId;
		User user = userRepository.findByPhone(userId);
		if (user == null) {
			log.warn("User with id {} not found", userId);
			return;
		}
		User friend = userRepository.findByPhone(friendId);
		if (friend == null) {
			log.warn("User with id {} not found", friendId);
			return;
		}
		Conversation conversation = conversationRepository.findById(conversationId);
		if (conversation == null) {
			conversation = conversationRepository.findById(friendId + "_" + userId);
			if (conversation == null) {
				
				log.warn("Conversation with id {} not found", conversationId);
				return;
			}
		}
		if (user.getConversations() != null) {
			log.info("User conversation {}", user.getConversations());
//			log.info(conversationId);
			user.removeConversationId(conversation.getId());
			log.info("User conversation after delete {}", user.getConversations());
		}
		if (friend.getConversations() != null) {
			friend.removeConversationId(conversation.getId());;
		}
		
		messageRepository.deleteMessagesByConversationId(conversation.getId());
		conversationRepository.deleteById(conversation.getId());
		userRepository.save(user);
		userRepository.save(friend);
		messageNotifier.notifyRemoveConversation(conversation.getId(), userId);
		messageNotifier.notifyRemoveConversation(conversation.getId(), friendId);
	}
	private void appendLastMessageIntoConversationDto(ConversationDto dto) {
		if (dto.getMessages() == null || dto.getMessages().isEmpty()) {
			dto.setLastMessage(null);
			return;
		}
		String lastMessageId = dto.getMessages().get(dto.getMessages().size() - 1);
		if (lastMessageId != null) {
			Message lastMessage = messageRepository.getMessageById(lastMessageId);
			MessageResponseDTO lastMessageDto = messageMapper.toMessageResponseDto(lastMessage);
			dto.setLastMessage(lastMessageDto);
		}
	}
	
	//Tạo gr
	@Override
    public String createGroup(String userId, CreateGroupRequest request) {
        if (!userService.isExistPhone(userId)) {
            log.warn("User with phone {} not found", userId);
            throw new IllegalArgumentException("Invalid user ID");
        }

        String conversationId = "group_" + UUID.randomUUID().toString();
        List<String> participants = new ArrayList<>(request.getMemberIds());
        participants.add(userId);

        // Xử lý upload ảnh
        String imageUrl = null;
        if (request.getBaseImg() != null && !request.getBaseImg().isEmpty()) {
            try {
                imageUrl = awsService.uploadToS3(request.getBaseImg());
                log.info("Uploaded group image to S3: {}", imageUrl);
            } catch (Exception e) {
                log.error("Failed to upload group image: {}", e.getMessage());
                throw new RuntimeException("Failed to upload group image", e);
            }
        }

        Conversation conversation = Conversation.builder()
            .id(conversationId)
            .type(ConversationType.GROUP)
            .participants(participants)
            .leader(userId)
            .admins(List.of(userId))
            .conversationName(request.getName() != null ? request.getName() : "Nhóm chưa đặt tên")
            .conversationImgUrl(imageUrl)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .messages(new ArrayList<>())
            .callInProgress(false)
            .currentCallId(null)
            .build();

        ConversationMember creator = ConversationMember.builder()
            .conversationId(conversationId)
            .userId(userId)
            .isAdmin(true)
            .joinedAt(LocalDateTime.now())
            .build();

        List<User> usersToUpdate = new ArrayList<>();
        User creatorUser = userRepository.findByPhone(userId);
        if (creatorUser != null) {
            if (creatorUser.getConversations() == null) {
                creatorUser.setConversations(new ArrayList<>());
            }
            creatorUser.getConversations().add(conversationId);
            usersToUpdate.add(creatorUser);
        }

        for (String memberId : request.getMemberIds()) {
            if (userService.isExistPhone(memberId) && !memberId.equals(userId)) {
                ConversationMember member = ConversationMember.builder()
                    .conversationId(conversationId)
                    .userId(memberId)
                    .isAdmin(false)
                    .joinedAt(LocalDateTime.now())
                    .build();
                conversationMemberRepository.save(member);

                User memberUser = userRepository.findByPhone(memberId);
                if (memberUser != null) {
                    if (memberUser.getConversations() == null) {
                        memberUser.setConversations(new ArrayList<>());
                    }
                    memberUser.getConversations().add(conversationId);
                    usersToUpdate.add(memberUser);
                }
            }
        }

        conversationMemberRepository.save(creator);
        conversationRepository.save(conversation);
        usersToUpdate.forEach(userRepository::save);

        ConversationDetailDto conversationDetailDto = getConversationDetail(conversationId);
        participants.forEach(participant ->
            messageNotifier.notifyNewConversation(conversationDetailDto, participant));
        log.info("Created group conversation {}", conversationId);
        return conversationId;
    }
	//thêm tv
	@Override
    public void addMembers(String userId, String conversationId, List<String> memberIds) {
        Conversation conversation = conversationRepository.findById(conversationId);
        if (conversation == null || conversation.getType() != ConversationType.GROUP) {
            log.warn("Group conversation {} not found", conversationId);
            throw new IllegalArgumentException("Group conversation not found");
        }
        if (!conversation.getAdmins().contains(userId) && !conversation.getLeader().equals(userId)) {
            log.warn("User {} is not authorized to add members to conversation {}", userId, conversationId);
            throw new SecurityException("Only admin or leader can add members");
        }

        List<String> addedMembers = new ArrayList<>();
        List<User> usersToUpdate = new ArrayList<>();
        for (String memberId : memberIds) {
            if (userService.isExistPhone(memberId) &&
                !conversationMemberRepository.exists(conversationId, memberId)) {
                ConversationMember member = ConversationMember.builder()
                    .conversationId(conversationId)
                    .userId(memberId)
                    .isAdmin(false)
                    .joinedAt(LocalDateTime.now())
                    .build();
                conversationMemberRepository.save(member);
                conversation.getParticipants().add(memberId);

                User user = userRepository.findByPhone(memberId);
                if (user != null) {
                    if (user.getConversations() == null) {
                        user.setConversations(new ArrayList<>());
                    }
                    user.getConversations().add(conversationId);
                    usersToUpdate.add(user);
                }
                addedMembers.add(memberId);
            }
        }

        if (!addedMembers.isEmpty()) {
            conversation.setUpdatedAt(LocalDateTime.now());
            conversationRepository.save(conversation);
            usersToUpdate.forEach(userRepository::save);
            messageNotifier.notifyGroupEvent(conversationId, "member_added", addedMembers);
            log.info("Added {} members to conversation {}", addedMembers.size(), conversationId);
        }
    }
	//xoá tv
	@Override
    public void removeMember(String userId, String conversationId, String targetUserId) {
        Conversation conversation = conversationRepository.findById(conversationId);
        if (conversation == null || conversation.getType() != ConversationType.GROUP) {
            log.warn("Group conversation {} not found", conversationId);
            throw new IllegalArgumentException("Group conversation not found");
        }
        if (!conversation.getAdmins().contains(userId) && !conversation.getLeader().equals(userId)) {
            log.warn("User {} is not authorized to remove members from conversation {}", userId, conversationId);
            throw new SecurityException("Only admin or leader can remove members");
        }
        if (!conversation.getParticipants().contains(targetUserId)) {
            log.warn("User {} is not a member of conversation {}", targetUserId, conversationId);
            throw new IllegalArgumentException("User is not a member");
        }

        conversationMemberRepository.delete(conversationId, targetUserId);
        conversation.getParticipants().remove(targetUserId);
        if (conversation.getAdmins().contains(targetUserId)) {
            conversation.getAdmins().remove(targetUserId);
        }

        User targetUser = userRepository.findByPhone(targetUserId);
        if (targetUser != null && targetUser.getConversations() != null) {
            targetUser.getConversations().remove(conversationId);
            userRepository.save(targetUser);
        }

        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
        messageNotifier.notifyGroupEvent(conversationId, "member_removed", List.of(targetUserId));
        log.info("Removed user {} from conversation {}", targetUserId, conversationId);
    }
//Cấp quyền admin
    @Override
    public void updateAdmin(String userId, String conversationId, String targetUserId, boolean isAdmin) {
        Conversation conversation = conversationRepository.findById(conversationId);
        if (conversation == null || conversation.getType() != ConversationType.GROUP) {
            log.warn("Group conversation {} not found", conversationId);
            throw new IllegalArgumentException("Group conversation not found");
        }
        if (!conversation.getLeader().equals(userId)) {
            log.warn("User {} is not authorized to update admin status in conversation {}", userId, conversationId);
            throw new SecurityException("Only leader can update admin status");
        }
        if (!conversation.getParticipants().contains(targetUserId)) {
            log.warn("User {} is not a member of conversation {}", targetUserId, conversationId);
            throw new IllegalArgumentException("User is not a member");
        }

        conversationMemberRepository.updateAdmin(conversationId, targetUserId, isAdmin);
        List<String> admins = new ArrayList<>(conversation.getAdmins());
        if (isAdmin && !admins.contains(targetUserId)) {
            admins.add(targetUserId);
        } else if (!isAdmin && admins.contains(targetUserId)) {
            admins.remove(targetUserId);
        }
        conversation.setAdmins(admins);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
        messageNotifier.notifyGroupEvent(conversationId, "admin_updated",
            List.of(targetUserId, String.valueOf(isAdmin)));
        log.info("Updated admin status for user {} in conversation {}: isAdmin={}",
            targetUserId, conversationId, isAdmin);
    }
//Thay đổi ảnh gr, tên gr
    @Override
    public void updateGroupInfo(String userId, String conversationId, String conversationName, String conversationImgUrl) {
        Conversation conversation = conversationRepository.findById(conversationId);
        if (conversation == null || conversation.getType() != ConversationType.GROUP) {
            log.warn("Group conversation {} not found", conversationId);
            throw new IllegalArgumentException("Group conversation not found");
        }
        if (!conversation.getAdmins().contains(userId) && !conversation.getLeader().equals(userId)) {
            log.warn("User {} is not authorized to update group info for conversation {}", userId, conversationId);
            throw new SecurityException("Only admin or leader can update group info");
        }

        if (conversationName != null && !conversationName.trim().isEmpty()) {
            conversation.setConversationName(conversationName);
        }
        if (conversationImgUrl != null) {
            conversation.setConversationImgUrl(conversationImgUrl);
        }
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
        messageNotifier.notifyGroupEvent(conversationId, "group_info_updated",
            List.of(conversationName, conversationImgUrl));
        log.info("Updated group info for conversation {}", conversationId);
    }
//Tìm thành viên
    @Override
    public List<MemberDto> searchMembers(String conversationId, String keyword) {
        Conversation conversation = conversationRepository.findById(conversationId);
        if (conversation == null || conversation.getType() != ConversationType.GROUP) {
            log.warn("Group conversation {} not found", conversationId);
            throw new IllegalArgumentException("Group conversation not found");
        }
        List<ConversationMember> members = conversationMemberRepository.searchMembers(conversationId, keyword);
        List<MemberDto> memberDtos = new ArrayList<>();
        for (ConversationMember member : members) {
            try {
                UserResponseDto userDto = userService.getUserInfo(member.getUserId());
                if (userDto != null) {
                    MemberDto memberDto = MemberDto.builder()
                        .phoneNumber(member.getUserId())
                        .name(userDto.getName())
                        .isAdmin(member.isAdmin())
                        .baseImg(userDto.getBaseImg())
                        .isOnline(userDto.isOnline())
                        .build();
                    memberDtos.add(memberDto);
                }
            } catch (Exception e) {
                log.warn("Error fetching user info for userId={}: {}", member.getUserId(), e.getMessage());
            }
        }
        log.info("Found {} members matching keyword '{}' in conversation {}", memberDtos.size(), keyword, conversationId);
        return memberDtos;
    }
//Rời gr
    @Override
    public void leaveGroup(String userId, String conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId);
        if (conversation == null || conversation.getType() != ConversationType.GROUP) {
            log.warn("Group conversation {} not found", conversationId);
            throw new IllegalArgumentException("Group conversation not found");
        }
        if (!conversation.getParticipants().contains(userId)) {
            log.warn("User {} is not a member of conversation {}", userId, conversationId);
            throw new IllegalArgumentException("User is not a member");
        }

        if (conversation.getLeader().equals(userId)) {
            if (conversation.getParticipants().size() > 1) {
                String newLeader = conversation.getAdmins().stream()
                    .filter(admin -> !admin.equals(userId))
                    .findFirst()
                    .orElse(conversation.getParticipants().stream()
                        .filter(p -> !p.equals(userId))
                        .findFirst()
                        .orElse(null));
                if (newLeader != null) {
                    conversation.setLeader(newLeader);
                    conversationMemberRepository.updateAdmin(conversationId, newLeader, true);
                    log.info("Transferred leadership to {} in conversation {}", newLeader, conversationId);
                }
            } else {
                conversation.getParticipants().forEach(participant -> {
                    User user = userRepository.findByPhone(participant);
                    if (user != null && user.getConversations() != null) {
                        user.getConversations().remove(conversationId);
                        userRepository.save(user);
                    }
                });
                messageRepository.deleteMessagesByConversationId(conversationId);
                conversationRepository.deleteById(conversationId);
                conversation.getParticipants().forEach(participant ->
                    messageNotifier.notifyRemoveConversation(conversationId, participant));
                log.info("Dissolved group conversation {}", conversationId);
                return;
            }
        }

        conversationMemberRepository.delete(conversationId, userId);
        conversation.getParticipants().remove(userId);
        if (conversation.getAdmins().contains(userId)) {
            conversation.getAdmins().remove(userId);
        }

        User user = userRepository.findByPhone(userId);
        if (user != null && user.getConversations() != null) {
            user.getConversations().remove(conversationId);
            userRepository.save(user);
        }

        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
        messageNotifier.notifyGroupEvent(conversationId, "member_left", List.of(userId));
        log.info("User {} left conversation {}", userId, conversationId);
    }

    //Lấy ds thành viên
    @Override
    public List<MemberDto> getGroupMembers(String conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId);
        if (conversation == null || conversation.getType() != ConversationType.GROUP) {
            log.warn("Group conversation {} not found", conversationId);
            throw new IllegalArgumentException("Group conversation not found");
        }

        List<ConversationMember> members = conversationMemberRepository.findByConversationId(conversationId);
        List<MemberDto> memberDtos = new ArrayList<>();

        for (ConversationMember member : members) {
            try {
                UserResponseDto userDto = userService.getUserInfo(member.getUserId());
                if (userDto != null) {
                    MemberDto memberDto = MemberDto.builder()
                        .phoneNumber(member.getUserId())
                        .name(userDto.getName())
                        .isAdmin(member.isAdmin())
                        .baseImg(userDto.getBaseImg())
                        .isOnline(userDto.isOnline())
                        .build();
                    memberDtos.add(memberDto);
                }
            } catch (Exception e) {
                log.warn("Error fetching user info for userId={}: {}", member.getUserId(), e.getMessage());
            }
        }

        log.info("Retrieved {} members for conversation {}", memberDtos.size(), conversationId);
        return memberDtos;
    }
    
}
