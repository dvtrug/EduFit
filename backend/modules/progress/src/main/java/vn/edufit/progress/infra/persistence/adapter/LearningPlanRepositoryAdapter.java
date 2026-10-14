package vn.edufit.progress.infra.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edufit.progress.domain.model.LearningPlan;
import vn.edufit.progress.domain.repository.LearningPlanDomainRepository;
import vn.edufit.progress.infra.persistence.entity.LearningPlanJpaEntity;
import vn.edufit.progress.infra.persistence.mapper.ProgressMapper;
import vn.edufit.progress.infra.persistence.repository.SpringDataLearningPlanRepository;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LearningPlanRepositoryAdapter implements LearningPlanDomainRepository {

  private final SpringDataLearningPlanRepository springDataRepository;
  private final ProgressMapper mapper;

  @Override
  public Optional<LearningPlan> findByClassId(UUID classId) {
    return springDataRepository.findByClassIdWithMilestones(classId)
        .map(mapper::toDomain);
  }

  @Override
  public Optional<LearningPlan> findById(UUID planId) {
    return springDataRepository.findByIdWithMilestones(planId)
        .map(mapper::toDomain);
  }

  @Override
  public Optional<LearningPlan> findByMilestoneId(UUID milestoneId) {
    return springDataRepository.findByMilestoneIdWithMilestones(milestoneId)
        .map(mapper::toDomain);
  }

  @Override
  public boolean existsByClassId(UUID classId) {

    return springDataRepository.existsByClassId(classId);
  }

  @Override
  public LearningPlan save(LearningPlan plan) {
    LearningPlanJpaEntity entity = mapper.toJpaEntity(plan);
    LearningPlanJpaEntity saved = springDataRepository.saveAndFlush(entity);
    return mapper.toDomain(saved);
  }
}
