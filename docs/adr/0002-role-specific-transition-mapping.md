# ADR 0002: Role-Specific Transition Mapping

## Status

Accepted.

## Context

The workflow permission model uses action-specific Role Access as the base permission gate. A user may perform a workflow action only when the backend grants permission for that action key, such as a fixed workflow action or a generic workflow action.

Role-specific transition mapping is a narrower, optional concern. It answers whether a role that already has action permission may see or execute a specific configured workflow transition for that action key.

The shipped system includes optional transition role scope through `workflow_transition_role_rules`, backend transition role rule APIs, frontend Role Access controls, validation advisories, and generic transition runtime enforcement.

## Decision

Action-specific Role Access remains the base workflow permission model.

Transition role scope is accepted as optional advanced workflow access control.

If no active transition role rules exist for a transition, the transition is open to all roles that already have action permission for the transition action key.

If one or more active transition role rules exist for a transition, only roles with active scope for that transition may use it. Transition scope narrows action permission; it does not replace action permission.

Missing transition role scope is not a validation blocker. Validation may show an informational advisory to explain that the transition is open to action-authorized roles because no transition-specific role restriction is configured.

Fixed protected actions remain outside transition-role mapping. Their behavior is handled by dedicated backend workflow paths and must not be replaced by dynamic transition scoping.

## Permission Filtering Versus Transition-Level Scoping

Action permission answers:

- Can this employee perform this action key at all?

Transition-level role scoping answers:

- For this allowed action key, can this employee role see or execute this specific configured workflow transition?

These are separate concerns and both must pass when transition scope rules are configured. Runtime generic transition execution checks action permission first, then optional transition scope.

## Transition Scope Model

Transition role scope uses a separate mapping table:

- `workflow_transition_role_rules`
- `workflow_transition_id`
- `role_id`
- `active`
- audit fields

Do not add role columns directly to `WorkflowTransition`.

A separate table supports many-to-many role scoping, avoids bloating `WorkflowTransition`, keeps the design migration-safe, and avoids mixing action permission rules with transition visibility rules.

## Runtime Direction

- Generic transition execution requires action permission for the action key.
- When no active transition role rules exist for the transition, transition scope allows all action-authorized roles.
- When active transition role rules exist for the transition, the employee role must have an active rule for that transition.
- Preview and execute paths should deny role-scope failures safely.
- Dynamic transition visibility should stay consistent with backend execution rules.

This keeps backend behavior consistent across visibility, preview, and execution.

## SUPER_ADMIN Behavior

- `SUPER_ADMIN` continues under the existing Role Access and permission behavior already implemented.
- `SUPER_ADMIN` should respect active transition configuration.
- Any admin-only override behavior must be separately approved, documented, and audited.

## Fail-Closed Behavior

- Missing action permission fails closed.
- Inactive transition fails closed.
- Inactive action metadata fails closed.
- When active transition scope rules exist, missing employee role scope fails closed.
- When active transition scope rules exist, inactive employee role scope fails closed.

Absence of transition scope rules is intentionally permissive for transition scope only. It does not bypass action permission. Once active transition scope rules exist for a transition, no employee should become visible or executable for that transition because their role scope is absent or inactive.

## Validation Behavior

- Missing transition role scope is informational only.
- Missing transition role scope must not block validation readiness or category activation.
- Validation should describe missing transition scope as: no transition-specific restriction is configured, so action-authorized roles may use the transition.
- Validation may still block when no active role can execute the action, or when configured transition role scope conflicts with action permission.

## Compatibility And Out Of Scope

This ADR does not approve:

- custom status execution
- ticket status mutation behavior changes
- replacing fixed protected workflow actions with dynamic transition scope
- removing action-specific Role Access as the base permission model

Existing fixed workflow behavior remains governed by dedicated backend paths. Optional transition role scope applies to generic configured workflow transitions and narrows action permission only when transition scope rules are configured.
