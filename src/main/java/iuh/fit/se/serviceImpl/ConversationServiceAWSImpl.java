package iuh.fit.se.serviceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import iuh.fit.se.mapper.ConversationMapper;
import iuh.fit.se.mapper.MessageMapper;
import iuh.fit.se.mapper.UserMapper;
import iuh.fit.se.model.Conversation;
import iuh.fit.se.model.Message;
import iuh.fit.se.model.User;
import iuh.fit.se.model.dto.conversation.ConversationDetailDto;
import iuh.fit.se.model.dto.conversation.ConversationDto;
import iuh.fit.se.model.dto.conversation.CreateGroupImgDto;
import iuh.fit.se.model.dto.conversation.MemberDto;
import iuh.fit.se.model.dto.message.MessageResponseDTO;
import iuh.fit.se.model.enumObj.ConversationType;
import iuh.fit.se.repo.ConversationRepository;
import iuh.fit.se.repo.MessageRepository;
import iuh.fit.se.repo.UserRepository;
import iuh.fit.se.service.ConversationService;
import iuh.fit.se.service.MessageNotifier;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConversationServiceAWSImpl implements ConversationService {

	@Value("${aws.region}")
	private String region;
	final ConversationRepository conversationRepository;
	final MessageRepository messageRepository;
	final UserRepository userRepository;
	final DynamoDbTable<User> userTable;
	final DynamoDbTable<Conversation> conversationTable;
	final DynamoDbEnhancedClient enhancedClient;
	final MessageNotifier messageNotifier;
	final MessageMapper messageMapper;
	final ConversationMapper conversationMapper;
	final AwsService awsService;
	final UserMapper userMapper;

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

		if (user.getConversations().contains(conversationId)) {
			log.warn("Conversation already exists");
			return;
		}
		if (friend.getConversations().contains(conversationId)) {
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
				.id(userPhone + "_" + friendPhone).callInProgress(false)
				.type(ConversationType.PRIVATE)
				.participants(List.of(userPhone, friendPhone))
				.createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
				.conversationName("ban").conversationImgUrl("xxx")
				.leader(userPhone).admins(List.of(userPhone, friendPhone))
				.messages(List.of()).currentCallId(null).build();
		try {
			enhancedClient.transactWriteItems(request -> request
					.addPutItem(conversationTable, conversation)
					.addPutItem(userTable, user).addPutItem(userTable, friend));
			log.info("Transaction completed successfully");
			ConversationDto dto = conversationMapper
					.fromConversationToDto(conversation);
			redefineConversationNameAndImgUrl(dto, userPhone);
			messageNotifier.notifyNewConversation(dto,
					userPhone);
			messageNotifier.notifyNewConversation(dto,
					friendPhone);

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
			// List<Conversation> conversations =
			// conversationRepository.findByUserId(userId);
			User user = userRepository.findByPhone(phone);
			if (user == null) {
				log.warn("User with id {} not found", phone);
				return List.of();
			}
			List<Conversation> conversations = new ArrayList<>();
			for (String conversationId : user.getConversations()) {
				Conversation conversation = conversationRepository
						.findById(conversationId);
				if (conversation != null) {
					conversations.add(conversation);
				}
			}
			conversations.sort(
					(c1, c2) -> c2.getUpdatedAt().compareTo(c1.getUpdatedAt()));
			List<ConversationDto> conversationDtos = new ArrayList<>();
			for (Conversation conversation : conversations) {
				ConversationDto conversationDto = conversationMapper
						.fromConversationToDto(conversation);
				// Thêm tin nhắn cuối vào conversationDto
				appendLastMessageIntoConversationDto(conversationDto);
				redefineConversationNameAndImgUrl(conversationDto, phone);

				conversationDtos.add(conversationDto);
			}
			return conversationDtos;
		} catch (Exception e) {
			log.error("Error getting conversations: {}", e.getMessage());
			throw new RuntimeException("Error getting conversations", e);
		}
	}

	@Override
	public ConversationDetailDto getConversationDetail(String conversationId,
			String phone) {
		
		ConversationDetailDto conversationDetailDto = getConversationDetail(conversationId);
		log.info("Conversation detail before redefine: {}",
				conversationDetailDto);
		redefineConversationNameAndImgUrl(conversationDetailDto, phone);

		log.info("Conversation detail after redefine: {}",
				conversationDetailDto);

		return conversationDetailDto;
	}

	@Override
	public ConversationDetailDto getConversationDetail(String conversationId) {
		// TODO Auto-generated method stub
		Conversation conversation = conversationRepository
				.findById(conversationId);
		if (conversation == null) {
			// log.warn("Conversation with id {} not found", conversationId);
			throw new RuntimeException("Conversation not found");
		}
		log.info(conversation.toString());
		ConversationDetailDto conversationDetailDto = conversationMapper
				.fromConversationToDetailDto(conversation);


		List<Message> messagesList = messageRepository
				.findMessagesByConversationId(conversationId);
		List<MessageResponseDTO> messages = new ArrayList<>();

		for (Message message : messagesList) {
			MessageResponseDTO messageResponseDTO = messageMapper
					.toMessageResponseDto(message);
			messages.add(messageResponseDTO);
		}
		if (conversation.getType() == ConversationType.GROUP) {
			List<MemberDto> members = getGroupMembers(conversationId);
			conversationDetailDto.setParticipantsDetails(members);
		} 
		
		conversationDetailDto.setMessageDetails((messages));

		return conversationDetailDto;
	}

	@Override
	public void updateLastUpdated(String conversationId) {
		// TODO Auto-generated method stub
		Conversation conversation = conversationRepository
				.findById(conversationId);
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

		Conversation conversation = conversationRepository
				.findById(conversationId);
		if (conversation == null) {
			log.warn("Conversation with id {} not found", conversationId);
			throw new RuntimeException("Conversation not found");
		}
		List<String> messageIds = conversation.getMessages();
		if (messageIds == null || messageIds.isEmpty()) {
			log.warn("No messages found in conversation with id {}", conversationId);
			return;
		}
		messageIds.stream().forEach(messageId -> {
			Message message = messageRepository.getMessageById(messageId);
			if (message != null && !message.getSeenBy().contains(userId)) {
				message.getSeenBy().add(userId);
				messageRepository.save(message);
			}
		});
		// Notify the user about the read status
		messageNotifier.notifyAllMessagesRead(conversationId, userId);
		log.info("All messages in conversation {} marked as read by user {}",
				conversationId, userId);
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
		Conversation conversation = conversationRepository
				.findById(conversationId);
		if (conversation == null) {
			conversation = conversationRepository
					.findById(friendId + "_" + userId);
			if (conversation == null) {

				log.warn("Conversation with id {} not found", conversationId);
				return;
			}
		}
		if (user.getConversations() != null) {
			log.info("User conversation {}", user.getConversations());
			// log.info(conversationId);
			user.removeConversationId(conversation.getId());
			log.info("User conversation after delete {}",
					user.getConversations());
		}
		if (friend.getConversations() != null) {
			friend.removeConversationId(conversation.getId());;
		}

		messageRepository.deleteMessagesByConversationId(conversation.getId());
		conversationRepository.deleteById(conversation.getId());
		userRepository.save(user);
		userRepository.save(friend);
		messageNotifier.notifyRemoveConversation(conversation.getId());
	}

	private void appendLastMessageIntoConversationDto(ConversationDto dto) {
		if (dto.getMessages() == null || dto.getMessages().isEmpty()) {
			dto.setLastMessage(null);
			return;
		}
		String lastMessageId = dto.getMessages()
				.get(dto.getMessages().size() - 1);
		if (lastMessageId != null) {
			Message lastMessage = messageRepository
					.getMessageById(lastMessageId);
			MessageResponseDTO lastMessageDto = messageMapper
					.toMessageResponseDto(lastMessage);
			dto.setLastMessage(lastMessageDto);
		}
	}

	private void redefineConversationNameAndImgUrl(
			ConversationDetailDto conversationDto, String phone) {
		String conversationId = conversationDto.getId();
		// cài đặt lại conversation name và conversation img url
		if (conversationDto.getType() == ConversationType.PRIVATE) {
			String[] split = conversationId.split("_");
			String otherUserId = split[0].equals(phone) ? split[1] : split[0];
			User otherUser = userRepository.findByPhone(otherUserId);
			 log.info("otherUserId: {}", otherUserId);
			if (otherUser != null) {
				conversationDto.setConversationName(otherUser.getName());
				conversationDto.setConversationImgUrl(otherUser.getBaseImg());
			} else {
				log.warn("User with id {} not found", otherUserId);
			}
		}
	}

	private void redefineConversationNameAndImgUrl(
			ConversationDto conversationDto, String phone) {
		String conversationId = conversationDto.getId();
		// cài đặt lại conversation name và conversation img url
		if (conversationDto.getType() == ConversationType.PRIVATE) {
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
	}
	@Override
	public ConversationDto getConversationById(String conversationId) {
		// TODO Auto-generated method stub

		Conversation conversation = conversationRepository
				.findById(conversationId);
		if (conversation == null) {
			log.warn("Conversation with id {} not found", conversationId);
			throw new RuntimeException("Conversation not found");
		}
		ConversationDto conversationDto = conversationMapper
				.fromConversationToDto(conversation);
		appendLastMessageIntoConversationDto(conversationDto);
		return conversationDto;
	}
	@Override
	public void updateConversationInCall(String conversationId,
			boolean inCall) {
		// TODO Auto-generated method stub
		Conversation conversation = conversationRepository
				.findById(conversationId);
		if (conversation == null) {
			log.warn("Conversation with id {} not found", conversationId);
			throw new RuntimeException("Conversation not found");
		}
		conversation.setCallInProgress(inCall);
		conversationRepository.save(conversation);
		log.info("Conversation {} updated to inCall: {}", conversationId,
				inCall);
	}

	public ConversationDetailDto createGroupChat(String creatorPhone,
			String conversationName, String conversationImgUrl,
			List<String> participants) {
		if (!participants.contains(creatorPhone)) {
			throw new IllegalArgumentException(
					"Creator must be in the participants list");
		}
		if (participants.size() < 3) {
			throw new IllegalArgumentException(
					"Group chat must have at least 3 members");
		}
		List<User> users = new ArrayList<>();
		for (String phone : participants) {
			User user = userRepository.findByPhone(phone);
			if (user == null) {
				throw new IllegalArgumentException(
						"User with phone " + phone + " not found");
			}
			users.add(user);
		}

		String baseId = LocalDateTime.now().toString().replace(":", "-") + "-"
				+ UUID.randomUUID();
		String conversationId = baseId;

		Conversation conversation = Conversation.builder().id(conversationId)
				.callInProgress(false).type(ConversationType.GROUP)
				.participants(new ArrayList<>(participants))
				.createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
				.conversationName(conversationName)
				.conversationImgUrl(conversationImgUrl).leader(creatorPhone)
				.admins(List.of(creatorPhone)).messages(List.of())
				.currentCallId(null).build();
		conversationRepository.save(conversation);
		for (User user : users) {
			if (user.getConversations() == null) {
				user.setConversations(new ArrayList<>());
			}
			user.getConversations().add(conversationId);
			userRepository.save(user);
		}
		ConversationDetailDto conversationDetailDto = getConversationDetail(
				conversationId);
		ConversationDto conversationDto = conversationMapper
				.fromConversationToDto(conversation);
		for (String phone : participants) {
			messageNotifier.notifyNewConversation(conversationDto, phone);
		}
		return conversationDetailDto;
	}

	@Override
	public ConversationDetailDto createGroupChat(CreateGroupImgDto request,
			String creatorPhone) {
		// TODO Auto-generated method stubub
		if (!request.getParticipants().contains(creatorPhone)) {
			throw new IllegalArgumentException(
					"Creator must be in the participants list");
		}
		if (request.getParticipants().size() < 3) {
			throw new IllegalArgumentException(
					"Group chat must have at least 3 members");
		}

		try {
			String imgUrl = awsService
					.uploadToS3(request.getConversationImgUrl());
			 ConversationDetailDto groupChat = createGroupChat(creatorPhone, request.getConversationName(), imgUrl,
					request.getParticipants());
			return groupChat; 
		} catch (Exception e) {
			// TODO Auto-generated catch block
			throw new RuntimeException(
					"Error uploading image to S3: " + e.getMessage());
		}
	}

	@Override
	public void addMembersToGroup(String conversationId,
			List<String> newMemberPhones) {
		Conversation conversation = conversationRepository
				.findById(conversationId);
		if (conversation == null) {
			throw new RuntimeException("Conversation not found");
		}
		if (newMemberPhones == null || newMemberPhones.isEmpty()) {
			throw new RuntimeException("No new members provided");
		}

		List<String> addedMembers = new ArrayList<>();
		for (String memberPhone : newMemberPhones) {
			if (conversation.getParticipants().contains(memberPhone)) {
				log.warn("Member {} already in the group, skipping",
						memberPhone);
				continue;
			}
			User member = userRepository.findByPhone(memberPhone);
			if (member == null) {
				log.warn("User with phone {} not found, skipping", memberPhone);
				continue;
			}
			conversation.getParticipants().add(memberPhone);
			if (member.getConversations() == null) {
				member.setConversations(new ArrayList<>());
			}
			member.getConversations().add(conversationId);
			userRepository.save(member);
			addedMembers.add(memberPhone);
		}

		if (addedMembers.isEmpty()) {
			throw new RuntimeException(
					"No valid members were added to the group");
		}

		conversation.setUpdatedAt(LocalDateTime.now());
		conversationRepository.save(conversation);
		ConversationDetailDto dto = convertToConversationDetailDto(conversation);
//		for (String memberPhone : addedMembers) {
//			messageNotifier.notifyMemberAdded(conversationId, memberPhone);
//			// messageNotifier.notifyNewConversation(conversationDetailDto,
//			// memberPhone);
//		}
		messageNotifier.notifyConversationUpdate(dto);
	}

	public void removeMemberFromGroup(String conversationId,
			String removerPhone, String memberPhone) {
		Conversation conversation = conversationRepository
				.findById(conversationId);
		if (conversation == null) {
			throw new RuntimeException("Conversation not found");
		}
		// Ensure remover is a participant
		if (!conversation.getParticipants().contains(removerPhone)) {
			throw new RuntimeException("Remover is not a member of the group");
		}
		// Only leader and admins can remove members
		if (!conversation.getLeader().equals(removerPhone)
				&& !conversation.getAdmins().contains(removerPhone)) {
			throw new RuntimeException(
					"Only the leader and admins can remove members");
		}
		// Prevent removing the leader
		if (memberPhone.equals(conversation.getLeader())) {
			throw new RuntimeException("Cannot remove the leader");
		}
		// Only leader can remove admins
		if (conversation.getAdmins().contains(memberPhone)
				&& !conversation.getLeader().equals(removerPhone)) {
			throw new RuntimeException("Only the leader can remove admins");
		}
		if (!conversation.getParticipants().contains(memberPhone)) {
			throw new RuntimeException("Member not in the group");
		}
		if (conversation.getParticipants().size() <= 3) {
			throw new RuntimeException(
					"Cannot remove member from a group with only 3 members");
		}
		conversation.getParticipants().remove(memberPhone);
		// If member is an admin, remove from admins list
		if (conversation.getAdmins().contains(memberPhone)) {
			conversation.getAdmins().remove(memberPhone);
		}
		conversationRepository.save(conversation);
		User member = userRepository.findByPhone(memberPhone);
		if (member != null && member.getConversations() != null) {
			member.getConversations().remove(conversationId);
			userRepository.save(member);
		}
		ConversationDetailDto conversationDetailDto = conversationMapper
				.fromConversationToDetailDto(conversation);
		conversationDetailDto.setParticipantsDetails(getGroupMembers(conversationId));
		conversationDetailDto.setMessageDetails(messageRepository.findMessagesByConversationId(conversationId)
				.stream().map(messageMapper::toMessageResponseDto)
				.toList());
		conversationDetailDto.setUpdatedAt(conversation.getUpdatedAt());
//		messageNotifier.notifyMemberLeft(conversationId, memberPhone);
		messageNotifier.notifyConversationUpdate(conversationDetailDto);
	}

	//
	@Override
	public void leaveGroup(String conversationId, String userPhone,
			String newLeaderPhone) {
		Conversation conversation = conversationRepository
				.findById(conversationId);
		if (conversation == null) {
			throw new RuntimeException("Conversation not found");
		}
		if (!conversation.getParticipants().contains(userPhone)) {
			throw new RuntimeException("Member not in the group");
		}
		int participantCount = conversation.getParticipants().size();
		if (participantCount > 3) {
			if (conversation.getLeader().equals(userPhone)) {
				if (newLeaderPhone == null || newLeaderPhone.isEmpty()) {
					throw new RuntimeException(
							"New leader must be specified when the leader leaves");
				}
				if (!conversation.getParticipants().contains(newLeaderPhone)) {
					throw new RuntimeException(
							"New leader must be a current member of the group");
				}
				conversation.setLeader(newLeaderPhone);
				if (!conversation.getAdmins().contains(newLeaderPhone)) {
					conversation.getAdmins().add(newLeaderPhone);
				}
//				messageNotifier.notifyNewLeader(conversationId, newLeaderPhone);
				
				ConversationDetailDto conversationDetailDto = conversationMapper
						.fromConversationToDetailDto(conversation);
				conversationDetailDto.setParticipantsDetails(getGroupMembers(conversationId));
				messageNotifier.notifyConversationUpdate(conversationDetailDto);
				
				
			}
			if (conversation.getAdmins().contains(userPhone)) {
				conversation.getAdmins().remove(userPhone);
			}
			conversation.getParticipants().remove(userPhone);

			conversationRepository.save(conversation);
			User member = userRepository.findByPhone(userPhone);
			if (member != null && member.getConversations() != null) {
				member.getConversations().remove(conversationId);
				userRepository.save(member);
			}
			messageNotifier.notifyMemberLeft(conversationId,
					member.getPhoneNumber());
		} else if (participantCount == 3) {
			// Disband the group by removing the conversation from all
			// participants' lists
			List<String> participants = new ArrayList<>(
					conversation.getParticipants());
			for (String participantPhone : participants) {
				User user = userRepository.findByPhone(participantPhone);
				if (user != null && user.getConversations() != null) {
					user.getConversations().remove(conversationId);
					userRepository.save(user);
				}
			}
			if (conversation.getConversationImgUrl() != null) { awsService.deleteFromS3(conversation.getConversationImgUrl()); }
			conversationRepository.deleteById(conversationId);
//			messageNotifier.notifyRemoveConversation(conversation.getId());

			
			
		} else {
			throw new RuntimeException(
					"Group has less than 3 members, cannot leave");
		}
	}
	// thêm notify thành công
	public void deleteGroup(String conversationId, String leaderPhone) {
		Conversation conversation = conversationRepository
				.findById(conversationId);
		if (conversation == null) {
			throw new RuntimeException("Conversation not found");
		}
		if (!conversation.getLeader().equals(leaderPhone)) {
			throw new RuntimeException("Only the leader can delete the group");
		}
		List<String> participants = conversation.getParticipants();
		for (String participantPhone : participants) {
			User user = userRepository.findByPhone(participantPhone);
			if (user != null && user.getConversations() != null) {
				user.getConversations().remove(conversationId);
				userRepository.save(user);
			}
		}
		conversationRepository.deleteById(conversationId);
		if (conversation.getConversationImgUrl() != null) { awsService.deleteFromS3(conversation.getConversationImgUrl()); }
		messageNotifier.notifyRemoveConversation(conversationId);

	}

	public void joinGroup(String conversationId, String userPhone) {
		Conversation conversation = conversationRepository
				.findById(conversationId);
		if (conversation == null) {
			throw new RuntimeException("Conversation not found");
		}
		if (!conversation.getType().equals(ConversationType.GROUP)) {
			throw new RuntimeException("Can only join group conversations");
		}

		if (conversation.getParticipants().contains(userPhone)) {
			throw new RuntimeException("User is already a member of the group");
		}

		User user = userRepository.findByPhone(userPhone);
		if (user == null) {
			throw new RuntimeException(
					"User with phone " + userPhone + " not found");
		}

		conversation.getParticipants().add(userPhone);
		conversationRepository.save(conversation);

		if (user.getConversations() == null) {
			user.setConversations(new ArrayList<>());
		}
		user.getConversations().add(conversationId);
		userRepository.save(user);

		// messageNotifier.notifyMemberAdded(conversationId, userPhone);
	}

	// update admin

	@Override
	public void updateAdmin(String userId, String conversationId,
			String targetUserId, boolean isAdmin) {
		Conversation conversation = conversationRepository
				.findById(conversationId);
		if (conversation == null
				|| conversation.getType() != ConversationType.GROUP) {
			log.warn("Group conversation {} not found", conversationId);
			throw new IllegalArgumentException("Group conversation not found");
		}
		if (!conversation.getLeader().equals(userId)) {
			log.warn(
					"User {} is not authorized to update admin status in conversation {}",
					userId, conversationId);
			throw new SecurityException("Only leader can update admin status");
		}
		if (!conversation.getParticipants().contains(targetUserId)) {
			log.warn("User {} is not a member of conversation {}", targetUserId,
					conversationId);
			throw new IllegalArgumentException("User is not a member");
		}

		List<String> admins = conversation.getAdmins();
		if (isAdmin && !admins.contains(targetUserId)) {
			admins.add(targetUserId);
		} else if (!isAdmin && admins.contains(targetUserId)) {
			admins.remove(targetUserId);
		}
		else {
			throw new RuntimeException("User %s is already %s in conversation %s".formatted(targetUserId,
					isAdmin ? "admin" : "not admin", conversationId));
		}
		conversation.setAdmins(admins);
		conversation.setUpdatedAt(LocalDateTime.now());
		conversationRepository.save(conversation);
		
//		temporary
		ConversationDetailDto conversationDetailDto = conversationMapper
				.fromConversationToDetailDto(conversation);
		conversationDetailDto.setParticipantsDetails(getGroupMembers(conversationId));
		conversationDetailDto.setMessageDetails(messageRepository.findMessagesByConversationId(conversationId)
				.stream().map(messageMapper::toMessageResponseDto)
				.toList());
		conversationDetailDto.setUpdatedAt(conversation.getUpdatedAt());
		messageNotifier.notifyConversationUpdate(conversationDetailDto);
		// messageNotifier.notifyGroupEvent(conversationId, "admin_updated",
		// List.of(targetUserId, String.valueOf(isAdmin)));
		log.info(
				"Updated admin status for user {} in conversation {}: isAdmin={}",
				targetUserId, conversationId, isAdmin);
	}
	// Thay đổi ảnh gr, tên gr
	@Override
	public void updateGroupInfo(String userId, String conversationId,
			String conversationName, String conversationImgUrl) {
		Conversation conversation = conversationRepository
				.findById(conversationId);
		if (conversation == null
				|| conversation.getType() != ConversationType.GROUP) {
			log.warn("Group conversation {} not found", conversationId);
			throw new IllegalArgumentException("Group conversation not found");
		}
		if (!conversation.getAdmins().contains(userId)
				&& !conversation.getLeader().equals(userId)) {
			log.warn(
					"User {} is not authorized to update group info for conversation {}",
					userId, conversationId);
			throw new SecurityException(
					"Only admin or leader can update group info");
		}

		if (conversationName != null && !conversationName.trim().isEmpty()) {
			conversation.setConversationName(conversationName);
		}
		if (conversationImgUrl != null) {
			conversation.setConversationImgUrl(conversationImgUrl);
		}
		conversation.setUpdatedAt(LocalDateTime.now());
		conversationRepository.save(conversation);
		ConversationDetailDto dto = convertToConversationDetailDto(conversation);
		messageNotifier.notifyConversationUpdate(dto);
		// messageNotifier.notifyGroupEvent(conversationId,
		// "group_info_updated",
		// List.of(conversationName, conversationImgUrl));
		log.info("Updated group info for conversation {}", conversationId);
	}
	// Tìm thành viên
	// đã xong
	@Override
	public List<MemberDto> searchMembers(String conversationId,
			String keyword) {
		Conversation conversation = conversationRepository
				.findById(conversationId);
		if (conversation == null
				|| conversation.getType() != ConversationType.GROUP) {
			log.warn("Group conversation {} not found", conversationId);
			throw new IllegalArgumentException("Group conversation not found");
		}
		List<String> membersId = conversation.getParticipants();
		List<MemberDto> members = membersId.stream()
				.map(userId -> userRepository.findByPhone(userId))
				.filter(user -> user.getName().toLowerCase()
						.contains(keyword.toLowerCase()))
				.map(user -> {
					MemberDto memberDto = userMapper.toMemberDto(user);
					memberDto.setAdmin(conversation.getAdmins()
							.contains(memberDto.getPhoneNumber()));
					return memberDto;
				}).toList();

		log.info("Found {} members matching keyword '{}' in conversation {}",
				members.size(), keyword, conversationId);
		return members;
	}
	// Rời gr

	// Lấy ds thành viên
	// xong
	@Override
	public List<MemberDto> getGroupMembers(String conversationId) {
		Conversation conversation = conversationRepository
				.findById(conversationId);
		if (conversation == null
				|| conversation.getType() != ConversationType.GROUP) {
			log.warn("Group conversation {} not found", conversationId);
			throw new IllegalArgumentException("Group conversation not found");
		}
		
		List<String> membersId = conversation.getParticipants();
		List<MemberDto> memberDtos = membersId.stream()
				.map(userId -> userRepository.findByPhone(userId)).map(user -> {
					MemberDto memberDto = userMapper.toMemberDto(user);
					
					log.info("MemberDto: {}", memberDto);
					
					memberDto.setAdmin(conversation.getAdmins()
							.contains(memberDto.getPhoneNumber().toLowerCase()));
					
					memberDto.setLeader(conversation.getLeader().equalsIgnoreCase(user.getPhoneNumber()));
					return memberDto;
				})
				.sorted((m1, m2) -> {
					if (m1.isLeader() && !m2.isLeader()) {
						return -1;
					} else if (!m1.isLeader() && m2.isLeader()) {
						return 1;
					} else {
						if (m1.isAdmin() && !m2.isAdmin()) {
							return -1;
						} else if (!m1.isAdmin() && m2.isAdmin()) {
							return 1;
						} else {
							return m1.getName().compareTo(m2.getName());
						}
					}
				})
				.toList();
		return memberDtos;
	}
	private ConversationDetailDto convertToConversationDetailDto(
			Conversation conversation) {
		ConversationDetailDto conversationDetailDto = conversationMapper
				.fromConversationToDetailDto(conversation);
		conversationDetailDto.setParticipantsDetails(getGroupMembers(conversation.getId()));
		conversationDetailDto.setMessageDetails(messageRepository.findMessagesByConversationId(conversation.getId())
				.stream().map(messageMapper::toMessageResponseDto)
				.toList());
		conversationDetailDto.setUpdatedAt(conversation.getUpdatedAt());
		return conversationDetailDto;
	}
}
