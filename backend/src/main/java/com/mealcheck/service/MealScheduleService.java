package com.mealcheck.service;

import com.mealcheck.config.DemoAccountGuard;
import com.mealcheck.dto.MealHistoryDTO;
import com.mealcheck.dto.MealScheduleDTO;
import com.mealcheck.dto.MealScheduleParticipantDTO;
import com.mealcheck.entity.MealSchedule;
import com.mealcheck.entity.MealScheduleParticipant;
import com.mealcheck.entity.User;
import com.mealcheck.repository.MealScheduleParticipantRepository;
import com.mealcheck.repository.MealScheduleRepository;
import com.mealcheck.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MealScheduleService {
    
    private final MealScheduleRepository scheduleRepository;
    private final MealScheduleParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final DemoAccountGuard demoAccountGuard;
    
    public List<MealScheduleDTO> getAllSchedules() {
        return scheduleRepository.findAll().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    public List<MealScheduleDTO> getActiveSchedules() {
        return scheduleRepository.findByActiveTrue().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    public List<MealScheduleDTO> getUpcomingSchedules() {
        return scheduleRepository.findByMealDateGreaterThanEqualOrderByMealDateAsc(LocalDate.now()).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    public List<MealScheduleDTO> getSchedulesByDate(LocalDate date) {
        return scheduleRepository.findByMealDate(date).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    public List<MealScheduleDTO> getSchedulesByDate(LocalDate date, Long userId) {
        return scheduleRepository.findByMealDate(date).stream()
            .map(schedule -> convertToDTO(schedule, userId))
            .collect(Collectors.toList());
    }
    
    public MealScheduleDTO getScheduleById(Long id) {
        MealSchedule schedule = scheduleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("식사 스케줄을 찾을 수 없습니다: " + id));
        return convertToDTO(schedule);
    }
    
    @Transactional
    public MealScheduleDTO createSchedule(MealScheduleDTO dto, Long userId) {
        demoAccountGuard.checkNotDemoUser();
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));
        
        // 중복 체크
        scheduleRepository.findByMealDateAndMealType(dto.getMealDate(), dto.getMealType())
            .ifPresent(s -> {
                throw new RuntimeException("해당 날짜와 시간대에 이미 스케줄이 등록되어 있습니다");
            });
        
        MealSchedule schedule = new MealSchedule();
        // 프론트엔드에서 yyyy-MM-dd 형식의 순수 날짜(LocalDate)로 전달되며,
        // JacksonTimeConfig 에서 타임존 보정을 처리하므로 별도의 +1일 보정 없이 그대로 사용한다.
        schedule.setMealDate(dto.getMealDate());
        schedule.setMealType(dto.getMealType());
        schedule.setDescription(dto.getDescription());
        schedule.setActive(true);
        schedule.setCreatedBy(user);
        
        MealSchedule saved = scheduleRepository.save(schedule);
        // 디버깅용 로그: 들어온 날짜와 실제 저장된 날짜 비교
        log.info("createSchedule - requestedDate={}, persistedDate={}, id={}",
            dto.getMealDate(), saved.getMealDate(), saved.getId());
        return convertToDTO(saved);
    }
    
    @Transactional
    public MealScheduleDTO updateSchedule(Long id, MealScheduleDTO dto) {
        demoAccountGuard.checkNotDemoUser();
        MealSchedule schedule = scheduleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("식사 스케줄을 찾을 수 없습니다: " + id));
        
        schedule.setDescription(dto.getDescription());
        if (dto.getActive() != null) {
            schedule.setActive(dto.getActive());
        }
        
        MealSchedule updated = scheduleRepository.save(schedule);
        return convertToDTO(updated);
    }
    
    @Transactional
    public void deleteSchedule(Long id) {
        demoAccountGuard.checkNotDemoUser();
        if (!scheduleRepository.existsById(id)) {
            throw new RuntimeException("식사 스케줄을 찾을 수 없습니다: " + id);
        }
        // 참여자 정보도 함께 삭제
        participantRepository.deleteAll(participantRepository.findByScheduleId(id));
        scheduleRepository.deleteById(id);
    }
    
    // 참여자 관련 메서드
    public List<MealScheduleParticipantDTO> getParticipantsBySchedule(Long scheduleId) {
        return participantRepository.findByScheduleId(scheduleId).stream()
            .map(this::convertParticipantToDTO)
            .collect(Collectors.toList());
    }
    
    public List<MealScheduleParticipantDTO> getCheckedParticipants(Long scheduleId) {
        return participantRepository.findByScheduleIdAndCheckedTrue(scheduleId).stream()
            .map(this::convertParticipantToDTO)
            .collect(Collectors.toList());
    }

    /**
     * 해당 스케줄에서 아직 식사를 수령하지 않은(checked 가 false 이거나,
     * 아예 참여 기록이 없는) 활성 사용자 목록을 반환합니다.
     */
    public List<MealScheduleParticipantDTO> getUncheckedParticipants(Long scheduleId) {
        // 스케줄 존재 여부 검증
        MealSchedule schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new RuntimeException("식사 스케줄을 찾을 수 없습니다: " + scheduleId));

        // 활성/승인 사용자 전체
        List<User> activeUsers = userRepository.findByApprovedTrue().stream()
            .filter(User::getActive)
            .filter(user -> !DemoAccountGuard.DEMO_USERNAME.equals(user.getUsername()))
            .collect(Collectors.toList());

        // 해당 스케줄에 대한 기존 참여 정보 맵 (userId -> participant)
        Map<Long, MealScheduleParticipant> participantMap = participantRepository.findByScheduleId(schedule.getId()).stream()
            .collect(Collectors.toMap(p -> p.getUser().getId(), p -> p));

        List<MealScheduleParticipantDTO> result = new ArrayList<>();

        for (User user : activeUsers) {
            MealScheduleParticipant participant = participantMap.get(user.getId());

            // 이미 수령(checked=true) 한 사용자는 제외
            if (participant != null && Boolean.TRUE.equals(participant.getChecked())) {
                continue;
            }

            if (participant != null) {
                // 참여 기록은 있으나 미수령(checked=false) 인 경우
                result.add(convertParticipantToDTO(participant));
            } else {
                // 참여 기록 자체가 없는 사용자도 미수령자로 간주
                MealScheduleParticipantDTO dto = new MealScheduleParticipantDTO();
                dto.setId(null);
                dto.setScheduleId(schedule.getId());
                dto.setUserId(user.getId());
                dto.setUserName(user.getName());
                dto.setUserDepartment(user.getDepartment());
                dto.setChecked(false);
                dto.setNote(null);
                dto.setCreatedAt(null);
                dto.setUpdatedAt(null);
                result.add(dto);
            }
        }

        return result;
    }
    
    @Transactional
    public MealScheduleParticipantDTO checkParticipant(Long scheduleId, Long userId, String note) {
        demoAccountGuard.checkNotDemoUser();
        MealSchedule schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new RuntimeException("식사 스케줄을 찾을 수 없습니다: " + scheduleId));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));
        
        // 활성 사용자만 체크 가능
        if (!user.getActive()) {
            throw new RuntimeException("비활성 사용자는 식사 체크를 할 수 없습니다.");
        }
        
        MealScheduleParticipant participant = participantRepository
            .findByScheduleIdAndUserId(scheduleId, userId)
            .orElse(new MealScheduleParticipant());
        
        if (participant.getId() == null) {
            participant.setSchedule(schedule);
            participant.setUser(user);
        }
        
        participant.setChecked(true);
        participant.setNote(note);
        
        MealScheduleParticipant saved = participantRepository.save(participant);
        return convertParticipantToDTO(saved);
    }
    
    @Transactional
    public void uncheckParticipant(Long scheduleId, Long userId) {
        demoAccountGuard.checkNotDemoUser();
        MealScheduleParticipant participant = participantRepository
            .findByScheduleIdAndUserId(scheduleId, userId)
            .orElseThrow(() -> new RuntimeException("참여 정보를 찾을 수 없습니다"));
        
        participant.setChecked(false);
        participantRepository.save(participant);
    }
    
    private MealScheduleDTO convertToDTO(MealSchedule schedule) {
        return convertToDTO(schedule, null);
    }
    
    private MealScheduleDTO convertToDTO(MealSchedule schedule, Long userId) {
        MealScheduleDTO dto = new MealScheduleDTO();
        dto.setId(schedule.getId());
        // DB에 저장된 날짜가 하루 앞당겨져 있는 환경을 고려해,
        // 클라이언트로 내려줄 때는 +1일 보정해서 전달한다.
        dto.setMealDate(schedule.getMealDate() != null ? schedule.getMealDate().plusDays(1) : null);
        dto.setMealType(schedule.getMealType());
        dto.setDescription(schedule.getDescription());
        dto.setActive(schedule.getActive());
        dto.setCreatedById(schedule.getCreatedBy().getId());
        dto.setCreatedByName(schedule.getCreatedBy().getName());
        dto.setCreatedAt(schedule.getCreatedAt());
        
        // 통계 추가
        long totalActiveUsers = userRepository.findByApprovedTrue().stream()
            .filter(User::getActive)
            .filter(user -> !DemoAccountGuard.DEMO_USERNAME.equals(user.getUsername()))
            .count(); // 활성 사용자 수 (데모 계정 제외)
        long checkedCount = participantRepository.countByScheduleIdAndCheckedTrue(schedule.getId());
        dto.setTotalParticipants(totalActiveUsers);
        dto.setCheckedCount(checkedCount);
        
        // 현재 사용자의 체크 여부 추가
        if (userId != null) {
            boolean isChecked = participantRepository.findByScheduleIdAndUserId(schedule.getId(), userId)
                .map(MealScheduleParticipant::getChecked)
                .orElse(false);
            dto.setCurrentUserChecked(isChecked);
        }
        
        return dto;
    }
    
    private MealScheduleParticipantDTO convertParticipantToDTO(MealScheduleParticipant participant) {
        MealScheduleParticipantDTO dto = new MealScheduleParticipantDTO();
        dto.setId(participant.getId());
        dto.setScheduleId(participant.getSchedule().getId());
        dto.setUserId(participant.getUser().getId());
        dto.setUserName(participant.getUser().getName());
        dto.setUserDepartment(participant.getUser().getDepartment());
        dto.setChecked(participant.getChecked());
        dto.setNote(participant.getNote());
        dto.setCreatedAt(participant.getCreatedAt());
        dto.setUpdatedAt(participant.getUpdatedAt());
        return dto;
    }
    
    // 식사 기록 조회
    public List<MealHistoryDTO> getUserMealHistory(Long userId, LocalDate startDate, LocalDate endDate) {
        List<MealScheduleParticipant> participants;
        
        if (startDate != null && endDate != null) {
            // 특정 기간의 참여 기록 조회
            participants = participantRepository.findByUserId(userId).stream()
                .filter(p -> {
                    LocalDate mealDate = p.getSchedule().getMealDate();
                    return !mealDate.isBefore(startDate) && !mealDate.isAfter(endDate);
                })
                .collect(Collectors.toList());
        } else {
            participants = participantRepository.findByUserId(userId);
        }
        
        return participants.stream()
            .map(this::convertToMealHistoryDTO)
            .collect(Collectors.toList());
    }
    
    public List<MealHistoryDTO> getAllMealHistory(LocalDate startDate, LocalDate endDate) {
        // 조회 기간에 해당하는 스케줄 목록 조회
        List<MealSchedule> schedules;
        if (startDate != null && endDate != null) {
            schedules = scheduleRepository.findAll().stream()
                .filter(s -> !s.getMealDate().isBefore(startDate) && !s.getMealDate().isAfter(endDate))
                .collect(Collectors.toList());
        } else {
            schedules = scheduleRepository.findAll();
        }

        // 활성/승인 사용자 전체 (각 스케줄마다 동일 기준 사용)
        List<User> activeUsers = userRepository.findByApprovedTrue().stream()
            .filter(User::getActive)
            .filter(user -> !DemoAccountGuard.DEMO_USERNAME.equals(user.getUsername()))
            .collect(Collectors.toList());

        List<MealHistoryDTO> historyList = new ArrayList<>();

        for (MealSchedule schedule : schedules) {
            // 해당 스케줄의 기존 참여 정보 (userId -> participant)
            Map<Long, MealScheduleParticipant> participantMap = participantRepository.findByScheduleId(schedule.getId()).stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), p -> p));

            for (User user : activeUsers) {
                MealScheduleParticipant participant = participantMap.get(user.getId());

                if (participant != null) {
                    // 이미 참여 정보가 있는 경우 (수령 or 미수령)
                    historyList.add(convertToMealHistoryDTO(participant));
                } else {
                    // 참여 정보 자체가 없는 경우도 미수령자로 간주하여 기록 생성
                    MealHistoryDTO dto = new MealHistoryDTO();
                    dto.setId(null);
                    dto.setScheduleId(schedule.getId());
                    dto.setMealDate(schedule.getMealDate() != null ? schedule.getMealDate().plusDays(1) : null);
                    dto.setMealType(schedule.getMealType());
                    dto.setDescription(schedule.getDescription());
                    dto.setUserId(user.getId());
                    dto.setUserName(user.getName());
                    dto.setUserDepartment(user.getDepartment());
                    dto.setChecked(false);
                    dto.setNote(null);
                    dto.setCheckedAt(null);
                    historyList.add(dto);
                }
            }
        }

        return historyList;
    }

    private MealHistoryDTO convertToMealHistoryDTO(MealScheduleParticipant participant) {
        MealHistoryDTO dto = new MealHistoryDTO();
        dto.setId(participant.getId());
        dto.setScheduleId(participant.getSchedule().getId());
        dto.setMealDate(participant.getSchedule().getMealDate() != null
            ? participant.getSchedule().getMealDate().plusDays(1)
            : null);
        dto.setMealType(participant.getSchedule().getMealType());
        dto.setDescription(participant.getSchedule().getDescription());
        dto.setUserId(participant.getUser().getId());
        dto.setUserName(participant.getUser().getName());
        dto.setUserDepartment(participant.getUser().getDepartment());
        dto.setChecked(participant.getChecked());
        dto.setNote(participant.getNote());
        dto.setCheckedAt(participant.getUpdatedAt());
        return dto;
    }
}
