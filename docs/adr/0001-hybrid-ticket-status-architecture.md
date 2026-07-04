# ADR 0001: Hybrid Ticket Status Architecture

## Status

Accepted.

## Context

Tickets use a hybrid status model:

- `tickets.status` is the required enum-backed ticket status and remains the behavior source.
- `tickets.status_id` points at `workflow_statuses` and supports exact workflow metadata such as display name, active flag, terminal flag, sort order, and behavior bucket.
- `WorkflowTransition.fromStatus` and `WorkflowTransition.toStatus` remain enum-backed behavior fields.
- `WorkflowTransition.fromStatusRecord` and `WorkflowTransition.toStatusRecord` provide exact metadata references for those enum behavior transitions.

This model lets the backend preserve existing enum behavior while also showing and executing constrained custom workflow status metadata.

## Decision

`tickets.status` remains the behavior source.

`tickets.status_id` and `Ticket.statusRecord` preserve the exact workflow status metadata/display state.

Constrained generic custom status execution is accepted. Generic execution may update a ticket to a custom workflow status only when the custom status maps to a supported `TicketStatus` behavior bucket and the transition does not require fixed workflow side effects.

Metadata-only status mutation remains disallowed. Generic custom execution must update both the enum behavior field and the exact workflow status record.

Fixed protected workflow behavior remains separate and must not be replaced by generic custom execution.

## Accepted Runtime Boundary

Constrained generic custom status execution is accepted when:

- source status record is active
- target status record is active
- statuses are not protected fixed statuses
- source and target have valid behavior buckets
- current `tickets.status` matches the source behavior bucket
- current `tickets.status_id` exactly matches the source workflow status id
- transition is active
- category rule is enabled
- action is active
- action-specific Role Access passes
- optional transition role scope passes when configured
- target does not require unsupported fixed workflow side effects

Ticket Detail loads runtime-eligible dynamic actions from:

```text
GET /volt/tickets/{id}/available-actions
```

Backend dynamic action evaluation resolves the ticket's exact `status_id`, finds active transitions from that exact workflow status, skips protected fixed actions, calls generic transition preparation, and returns only runtime-eligible dynamic actions.

Dynamic execution uses:

```text
POST /volt/tickets/{id}/workflow-transitions/{transitionId}/execute
```

## Supported Generic Custom Paths

Supported custom paths are behavior-bucket backed and side-effect safe.

Example:

```text
CUSTOM_NEW behaviorBucket NEW
  -> CUSTOM_IN_PROGRESS behaviorBucket IN_PROGRESS
```

Example:

```text
CUSTOM_IN_PROGRESS behaviorBucket IN_PROGRESS
  -> CUSTOM_DONE behaviorBucket COMPLETED terminal=true
```

The terminal custom target must be an active custom status with `behaviorBucket = COMPLETED`.

## Explicitly Unsupported Generic Paths

Generic execution must not replace fixed workflow behavior.

Unsupported generic paths include:

- protected transitions
- protected actions
- fixed action keys:
  - `PICK_TICKET`
  - `START_WORK`
  - `COMPLETE_TICKET`
  - `CANCEL_TICKET`
- target `PICKED`
- target `CANCELLED`
- protected/system terminal `COMPLETED`
- terminal source statuses
- non-terminal target with terminal behavior bucket
- any path that requires fixed side effects such as ownership, completion fields, cancellation fields, or special authorization

## Dual-Field Mutation Rule

Generic custom execution updates both:

- `tickets.status`
- `tickets.status_id`

`tickets.status` is set to the target status record's `behaviorBucket`. `tickets.status_id` is set to the exact target `WorkflowStatus`.

This keeps enum status as the behavior source while preserving the exact custom workflow metadata/display state.

Metadata-only mutation of `tickets.status_id` remains disallowed unless a later ADR explicitly replaces the enum behavior source.

## Fixed Workflow Boundary

Fixed workflow actions still have behavior that is larger than a status update:

- pick ticket assigns ownership
- start work updates ownership and operational state
- complete ticket enforces authorization rules, then writes completion fields
- cancel ticket enforces admin authorization, then writes cancellation fields

Those behaviors remain in dedicated backend paths. Generic custom execution must not impersonate or replace them.

## Validation Gap

Known current gap:

```text
Validation readiness may not yet fully mirror GenericTransitionExecutorService runtime eligibility.
```

Workflow validation should be enhanced so `readyToActivate` means runtime-executable for generic custom transitions.

Until that hardening is complete, a workflow may be metadata-valid and reachable while a specific transition is still hidden or denied by runtime eligibility because it requires fixed workflow side effects.

## Compatibility And Out Of Scope

This ADR does not approve:

- replacing `tickets.status` as the behavior source
- metadata-only ticket status mutation
- replacing fixed workflow actions with generic custom execution
- generic execution of fixed side-effect paths
- expanding custom execution beyond the constrained behavior-bucket model documented here

Existing fixed workflows, workflow history reads, Ticket Detail display behavior, action-specific Role Access, and optional transition role scope remain compatible with this hybrid model.
