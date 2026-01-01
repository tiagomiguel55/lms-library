/*
 * Copyright (c) 2022-2024 the original author or authors.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package pt.psoft.g1.psoftg1.usermanagement.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.shared.repositories.ForbiddenNameRepository;
import pt.psoft.g1.psoftg1.shared.services.Page;
import pt.psoft.g1.psoftg1.usermanagement.api.UserViewAMQPMapper;
import pt.psoft.g1.psoftg1.usermanagement.model.Librarian;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.model.Role;
import pt.psoft.g1.psoftg1.usermanagement.model.User;
import pt.psoft.g1.psoftg1.usermanagement.api.UserViewAMQP;
import pt.psoft.g1.psoftg1.usermanagement.publishers.UserEventPublisher;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

import java.util.List;
import java.util.Optional;

/**
 * Based on https://github.com/Yoh0xFF/java-spring-security-example
 *
 */
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

	private final UserRepository userRepo;
	private final EditUserMapper userEditMapper;

	private final ForbiddenNameRepository forbiddenNameRepository;

	private final PasswordEncoder passwordEncoder;

	private final UserEventPublisher userEventPublisher;
	private final UserViewAMQPMapper userViewAMQPMapper;

	public List<User> findByName(String name){
		return this.userRepo.findByNameName(name);
	}
	public List<User> findByNameLike(String name) { return this.userRepo.findByNameNameContains(name); }

	@Transactional
	public User create(final CreateUserRequest request) {
		if (userRepo.findByUsername(request.getUsername()).isPresent()) {
			throw new ConflictException("Username already exists!");
		}

		Iterable<String> words = List.of(request.getName().split("\\s+"));
		for (String word : words){
			if(!forbiddenNameRepository.findByForbiddenNameIsContained(word).isEmpty()) {
				throw new IllegalArgumentException("Name contains a forbidden word");
			}
		}

		User user;
		switch(request.getRole()) {
			case Role.READER: {
				user = Reader.newReader(request.getUsername(), request.getPassword(), request.getName());
				break;
			}
			case Role.LIBRARIAN: {
				user = Librarian.newLibrarian(request.getUsername(), request.getPassword(), request.getName());
				break;
			}
			default: {
				return null;
			}
		}

		//final User user = userEditMapper.create(request);
		user.setPassword(passwordEncoder.encode(request.getPassword()));
		//user.addAuthority(new Role(request.getRole()));

		User savedUser = userRepo.save(user);

		// Publish user created event via AMQP
		try {
			userEventPublisher.sendUserCreated(userViewAMQPMapper.toUserViewAMQP(savedUser));
		} catch (Exception e) {
			// Log the error but don't fail the user creation
			System.out.println("Failed to publish user created event: " + e.getMessage());
		}

		return savedUser;
	}

	@Transactional
	public User update(final String id, final EditUserRequest request) {
		final User user = userRepo.getById(id);
		userEditMapper.update(request, user);

		User updatedUser = userRepo.save(user);

		// Publish user updated event via AMQP
		try {
			userEventPublisher.sendUserUpdated(userViewAMQPMapper.toUserViewAMQP(updatedUser));
		} catch (Exception e) {
			// Log the error but don't fail the user update
			System.out.println("Failed to publish user updated event: " + e.getMessage());
		}

		return updatedUser;
	}

	@Transactional
	public User delete(final String id) {
		final User user = userRepo.getById(id);
		String username = user.getUsername();

		// user.setUsername(user.getUsername().replace("@", String.format("_%s@",
		// user.getId().toString())));
		user.setEnabled(false);
		User deletedUser = userRepo.save(user);

		// Publish user deleted event via AMQP
		try {
			userEventPublisher.sendUserDeleted(username);
		} catch (Exception e) {
			// Log the error but don't fail the user deletion
			System.out.println("Failed to publish user deleted event: " + e.getMessage());
		}

		return deletedUser;
	}


	// AMQP Event Handlers

	/**
	 * Handles user created events from lms_auth_users module
	 */
	@Transactional
	public void handleUserCreated(UserViewAMQP userViewAMQP) {
		System.out.println("Processing user created event for username: " + userViewAMQP.getUsername());

		try {
			// Check if user already exists locally
			Optional<User> existingUser = userRepo.findByUsername(userViewAMQP.getUsername());
			if (existingUser.isPresent()) {
				System.out.println("User already exists locally, skipping creation: " + userViewAMQP.getUsername());
				return;
			}

			// Validate forbidden names
			Iterable<String> words = List.of(userViewAMQP.getFullName().split("\\s+"));
			for (String word : words) {
				if (!forbiddenNameRepository.findByForbiddenNameIsContained(word).isEmpty()) {
					System.out.println("User creation rejected - forbidden name: " + userViewAMQP.getFullName());
					return;
				}
			}

			// Create user locally based on the received event
			User user = Reader.newReader(
					userViewAMQP.getUsername(),
					userViewAMQP.getPassword() != null ? userViewAMQP.getPassword() : "EXTERNAL_AUTH",
					userViewAMQP.getFullName()
			);

			// If password is provided, encode it, otherwise use placeholder for external auth
			if (userViewAMQP.getPassword() != null) {
				user.setPassword(passwordEncoder.encode(userViewAMQP.getPassword()));
			}

			User savedUser = userRepo.save(user);
			System.out.println("User created locally from AMQP event: " + savedUser.getUsername());

		} catch (Exception e) {
			System.out.println("Error processing user created event: " + e.getMessage());
			System.out.println("Exception details: " + e.getClass().getSimpleName());
		}
	}

	/**
	 * Handles user updated events from lms_auth_users module
	 */
	@Transactional
	public void handleUserUpdated(UserViewAMQP userViewAMQP) {
		System.out.println("Processing user updated event for username: " + userViewAMQP.getUsername());

		try {
			// Find existing user
			Optional<User> existingUser = userRepo.findByUsername(userViewAMQP.getUsername());
			if (existingUser.isEmpty()) {
				System.out.println("User not found locally, cannot update: " + userViewAMQP.getUsername());
				return;
			}

			User user = existingUser.get();

			// Validate forbidden names for the new fullName
			if (userViewAMQP.getFullName() != null) {
				Iterable<String> words = List.of(userViewAMQP.getFullName().split("\\s+"));
				for (String word : words) {
					if (!forbiddenNameRepository.findByForbiddenNameIsContained(word).isEmpty()) {
						System.out.println("User update rejected - forbidden name: " + userViewAMQP.getFullName());
						return;
					}
				}

				// Update the full name
				user.getName().setName(userViewAMQP.getFullName());
			}

			// Update password if provided
			if (userViewAMQP.getPassword() != null && !userViewAMQP.getPassword().equals("EXTERNAL_AUTH")) {
				user.setPassword(passwordEncoder.encode(userViewAMQP.getPassword()));
			}

			User updatedUser = userRepo.save(user);
			System.out.println("User updated locally from AMQP event: " + updatedUser.getUsername());

		} catch (Exception e) {
			System.out.println("Error processing user updated event: " + e.getMessage());
			System.out.println("Exception details: " + e.getClass().getSimpleName());
		}
	}

	/**
	 * Handles user deleted events from lms_auth_users module
	 */
	@Transactional
	public void handleUserDeleted(String username) {
		System.out.println("Processing user deleted event for username: " + username);

		try {
			// Find existing user
			Optional<User> existingUser = userRepo.findByUsername(username);
			if (existingUser.isEmpty()) {
				System.out.println("User not found locally, cannot delete: " + username);
				return;
			}

			User user = existingUser.get();

			// Soft delete - disable the user instead of hard delete to maintain referential integrity
			user.setEnabled(false);

			User deletedUser = userRepo.save(user);
			System.out.println("User disabled locally from AMQP delete event: " + deletedUser.getUsername());

		} catch (Exception e) {
			System.out.println("Error processing user deleted event: " + e.getMessage());
			System.out.println("Exception details: " + e.getClass().getSimpleName());
		}
	}

	@Override
	public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
		return userRepo.findByUsername(username).orElseThrow(
				() -> new UsernameNotFoundException(String.format("User with username - %s, not found", username)));
	}

	public boolean usernameExists(final String username) {
		return userRepo.findByUsername(username).isPresent();
	}

	public User getUser(final String id) {
		return userRepo.getById(id);
	}

	public Optional<User> findByUsername(final String username) { return userRepo.findByUsername(username); }

	public List<User> searchUsers(Page page, SearchUsersQuery query) {
		if (page == null) {
			page = new Page(1, 10);
		}
		if (query == null) {
			query = new SearchUsersQuery("", "");
		}
		return userRepo.searchUsers(page, query);
	}

	public User getAuthenticatedUser(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
			throw new AccessDeniedException("User is not logged in");
		}

		// split is present because jwt is storing the id before the username, separated by a comma
        String loggedUsername = jwt.getClaimAsString("sub").split(",")[1];

		Optional<User> loggedUser = findByUsername(loggedUsername);
		if (loggedUser.isEmpty()) {
			throw new AccessDeniedException("User is not logged in");
		}

		return loggedUser.get();
	}
}
