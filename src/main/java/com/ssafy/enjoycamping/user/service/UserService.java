package com.ssafy.enjoycamping.user.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.ssafy.enjoycamping.common.exception.BadRequestException;
import com.ssafy.enjoycamping.common.exception.BaseException;
import com.ssafy.enjoycamping.common.exception.UnauthorizedException;
import com.ssafy.enjoycamping.common.response.BaseResponseStatus;
import com.ssafy.enjoycamping.trip.attraction.dto.AttractionDto;
import com.ssafy.enjoycamping.user.dto.*;
import com.ssafy.enjoycamping.user.util.*;
import org.springframework.stereotype.Service;

import com.ssafy.enjoycamping.user.dao.UserDao;
import com.ssafy.enjoycamping.user.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
	
	private final UserDao userDao;

	public JoinDto.ResponseJoinDto join(JoinDto.RequestJoinDto request) throws BaseException {
		// 이메일 중복 확인
		userDao.selectByEmail(EncryptionService.encrypt(request.getEmail()))
				.ifPresent(user -> {
					if(user.isDeleteFlag()) throw new BadRequestException(BaseResponseStatus.WITHDRAW_USER);
					else throw new BadRequestException(BaseResponseStatus.ALREADY_EXIST_EMAIL);
				});

		User user = User.builder()
				.name(request.getName())
				.email(request.getEmail())
				.password(request.getPassword())
				.build();
		
		// 이메일 양방향 암호화
	    String encryptedEmail = EncryptionService.encrypt(user.getEmail());
	    user.setEmail(encryptedEmail);
	    
	    // 비밀번호 단방향 해시 암호화
	    String hashedPassword = PasswordEncoder.encode(user.getPassword());
	    user.setPassword(hashedPassword);
	    
	    // DB에 유저 정보 저장
		userDao.insert(user);
		return JoinDto.ResponseJoinDto.builder()
				.id(user.getId())
				.build();
	}

	public LoginDto.ResponseLoginDto login(LoginDto.RequestLoginDto request) throws BaseException {
		// DB에서 암호화된 이메일로 유저 정보 조회
	    User user = userDao.selectActiveByEmail(EncryptionService.encrypt(request.getEmail()))
				.orElseThrow(() -> new BadRequestException(BaseResponseStatus.NOT_EXIST_EMAIL));

		// 비밀번호 일치 여부 조회
		if(!PasswordEncoder.matches(request.getPassword(), user.getPassword()))
			throw new BadRequestException(BaseResponseStatus.NOT_CORRECT_PASSWORD);

		// access token 생성
		String accessToken = JwtProvider.createAccessToken(JwtPayload.builder()
						.id(user.getId())
						.issuedAt(new Date())
						.build());
		// refresh token 생성
		String refreshToken = JwtProvider.createRefreshToken(JwtPayload.builder()
				.id(user.getId())
				.issuedAt(new Date())
				.build());
		// TODO: Refresh Token DB에 저장

		return LoginDto.ResponseLoginDto.builder()
				.accessToken(accessToken)
				.refreshToken(refreshToken)
				.build();
	}

	public LoginDto.ResponseLoginDto reissueToken() throws BaseException {
		int id = JwtProvider.getUserId();

		// JWT로 User 불러오기
		User user = userDao.selectActiveById(id)
				.orElseThrow(() -> new UnauthorizedException(BaseResponseStatus.INVALID_USER_JWT));
		
		// TODO: DB에서 User의 refresh token 조회해서 비교

		// access token 재발급
		String accessToken = JwtProvider.createAccessToken(JwtPayload.builder()
				.id(user.getId())
				.issuedAt(new Date())
				.build());
		// refresh token 재발급
		String refreshToken = JwtProvider.createRefreshToken(JwtPayload.builder()
				.id(user.getId())
				.issuedAt(new Date())
				.build());
		// TODO: Refresh Token DB에 저장

		return LoginDto.ResponseLoginDto.builder()
				.accessToken(accessToken)
				.refreshToken(refreshToken)
				.build();
	}

	public List<UserDto> getUsers() throws BaseException {
		return userDao.selectAll().stream()
				.map(UserDto::fromEntity)
				.collect(Collectors.toList());
	}

	public UserDto getUser(int id) throws BaseException {
		User user = userDao.selectById(id)
				.orElseThrow(() -> new BadRequestException(BaseResponseStatus.NOT_EXIST_USER));

		return UserDto.fromEntity(user);
	}

	public FindPwdDto.ResponseFindPwdDto findPassword(FindPwdDto.RequestFindPwdDto request) throws BaseException {
		int id = JwtProvider.getUserId();

		// JWT로 User 불러오기
		User user = userDao.selectActiveById(id)
				.orElseThrow(() -> new UnauthorizedException(BaseResponseStatus.INVALID_USER_JWT));

		// DB에서 암호화된 이메일로 유저 정보 조회
		User emailUser = userDao.selectActiveByEmail(EncryptionService.encrypt(request.getEmail()))
				.orElseThrow(() -> new BadRequestException(BaseResponseStatus.NOT_EXIST_EMAIL));

		// JWT 유저 권한 확인
		if(user.getId() != emailUser.getId()) throw new UnauthorizedException(BaseResponseStatus.INVALID_USER_JWT);
		// 이름 확인
		if(!user.getName().equals(emailUser.getName())) throw new BadRequestException(BaseResponseStatus.INPUT_PARSE_ERROR);

		// 새로운 랜덤 비밀번호 생성
	    String randomPassword = RandomPasswordBuilder.getRandomPassword(10);

		// 비밀번호 단방향 해시 암호화
		String hashedPassword = PasswordEncoder.encode(randomPassword);
		user.setPassword(hashedPassword);

		userDao.update(user);
		return FindPwdDto.ResponseFindPwdDto.builder()
				.password(randomPassword)
				.build();
	}

	public ModifyPwdDto.ResponseModifyPwdDto modifyPassword(ModifyPwdDto.RequestModifyPwdDto request) throws BaseException {
		int id = JwtProvider.getUserId();

		// JWT로 User 불러오기
		User user = userDao.selectActiveById(id)
				.orElseThrow(() -> new UnauthorizedException(BaseResponseStatus.INVALID_USER_JWT));

		// 비밀번호 일치 여부 조회
		if(!PasswordEncoder.matches(request.getPassword(), user.getPassword()))
			throw new BadRequestException(BaseResponseStatus.NOT_CORRECT_PASSWORD);

		// 비밀번호 단방향 해시 암호화
	    String hashedPassword = PasswordEncoder.encode(request.getNewPassword());
		user.setPassword(hashedPassword);
		
		userDao.update(user);
		return ModifyPwdDto.ResponseModifyPwdDto.builder()
				.id(user.getId())
				.build();
	}

	public void logout() throws BaseException {
		int id = JwtProvider.getUserId();

		// JWT로 User 불러오기
		User user = userDao.selectActiveById(id)
				.orElseThrow(() -> new UnauthorizedException(BaseResponseStatus.INVALID_USER_JWT));

		// TODO: Token 블랙리스트에 추가
	}

	public void withdraw() throws BaseException {
		int id = JwtProvider.getUserId();

		// JWT로 User 불러오기
		User user = userDao.selectActiveById(id)
				.orElseThrow(() -> new UnauthorizedException(BaseResponseStatus.INVALID_USER_JWT));

		user.setDeleteFlag(true);
		userDao.update(user);
	}
}
