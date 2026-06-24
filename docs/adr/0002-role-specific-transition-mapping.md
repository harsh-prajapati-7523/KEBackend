# ADR 0002: Role-Specific Transition Mapping

## Status

Accepted as backend documentation only. Runtime behavior, schema changes, and frontend changes remain deferred.

## Context

The current workflow permission model uses action-specific Role Access. A user may perform a workflow action only when the backend grants permission for that action key, such as a fixed workflow action or a generic workflow action.

Role-specific transition mapping is a different concern. It would answer whether a role may see or execute a specific configured workflow transition for an action key. This is narrower than action permission and should not be introduced unless Product confirms a business case where the same action key must expose different transitions by role.

## Decision

Action-specific Role Access remains the V1 workflow permission model.

Role-specific transition mapping is deferred. V1 must not duplicate Role Access rules or introduce transition-level role scoping without a confirmed product need.

Fixed protected actions remain outside transition-role mapping. Their behavior is handled by dedicated backend workflow paths and must not be replaced by dynamic transition scoping.

## Permission Filtering Versus Transition-Level Scoping

Permission filtering answers:

- Can this employee perform this action key at all?

Transition-level role scoping would answer:

- For this allowed action key, can this employee role see or execute this specific workflow transition?

These are separate concerns. Combining them too early would risk duplicating Role Access behavior, creating conflicting authorization paths, and making dynamic workflow visibility harder to reason about.

## Deferred Future Model

If Product later approves transition-level role scoping, prefer a separate mapping table:

- `workflow_transition_role_rules`
- `workflow_transition_id`
- `role_id` or role key
- `active`
- audit fields

Do not add role columns directly to `WorkflowTransition`.

A separate table supports many-to-many role scoping, avoids bloating `WorkflowTransition`, keeps the design migration-safe, and avoids mixing action permission rules with transition visibility rules.

## Future Runtime Direction

If role-specific transition mapping is approved later:

- `dynamicActions` should hide role-scoped transitions the employee cannot execute.
- preview should deny role-scope failures safely.
- execute should deny role-scope failures safely.
- one shared backend validator should be used by `dynamicActions`, preview, and execute.

This keeps backend behavior consistent across visibility, preview, and execution.

## SUPER_ADMIN Behavior

Current V1:

- `SUPER_ADMIN` continues under the existing Role Access and permission behavior already implemented.

Future scoped mode default:

- `SUPER_ADMIN` should respect active transition configuration.
- Any admin-only override behavior must be separately approved, documented, and audited.

## Fail-Closed Behavior

Current V1:

- Missing action permission fails closed.

Future scoped mode:

- missing mapping fails closed
- inactive mapping fails closed
- inactive transition fails closed
- inactive action metadata fails closed

No transition should become visible or executable because configuration is absent or inactive.

## Compatibility

This ADR does not approve or implement:

- `workflow_transition_role_rules` schema
- role-specific transition mapping runtime logic
- backend validator changes
- `dynamicActions` changes
- preview endpoint changes
- execute endpoint changes
- fixed workflow changes
- frontend changes

Existing Role Access behavior, `role_access_rules`, `access_key_metadata`, generic execution behavior, fixed workflow actions, and frontend behavior remain unchanged.
