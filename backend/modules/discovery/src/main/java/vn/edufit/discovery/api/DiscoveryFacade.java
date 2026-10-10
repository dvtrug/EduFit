package vn.edufit.discovery.api;

import java.util.List;
import java.util.UUID;
import vn.edufit.discovery.api.dto.TutorMatchSummaryDto;
import vn.edufit.shared.auth.CurrentUser;
import vn.edufit.shared.exception.EntityNotFoundException;
import vn.edufit.shared.exception.ForbiddenOperationException;
import vn.edufit.shared.exception.InvalidOperationException;

/** Public entry point for goal-based matching from other backend modules. */
public interface DiscoveryFacade {

  /**
   * Executes an authorized matching run and returns summaries in ranking order.
   * The run applies the actor's AI quota and records matching audit data.
   *
   * @param actor authenticated identity supplied by the trusted server context
   * @param goalId learning goal to match
   * @param topN maximum number of results, from 1 to 10; use 5 for the default
   * @return ranked summaries, or an empty list when no tutors qualify
   * @throws EntityNotFoundException if the learning goal does not exist
   * @throws ForbiddenOperationException if the actor cannot access the goal
   * @throws InvalidOperationException if the goal or matching input is invalid
   */
  List<TutorMatchSummaryDto> findTopMatchesForGoal(CurrentUser actor, UUID goalId, int topN);
}
