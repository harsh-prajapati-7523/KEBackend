# ADR 0001: Hybrid Ticket Status Architecture

## Status

Accepted as backend documentation and validation foundation. Custom status assignment execution remains deferred.

## Context

Tickets currently use a hybrid status model:

- `tickets.status` is the required enum-backed ticket status.
- `tickets.status_id` points at `workflow_statuses` and supports status metadata such as display name, active flag, terminal flag, sort order, and behavior bucket.
- `WorkflowTransition.fromStatus` and `WorkflowTransition.toStatus` remain enum-backed behavior fields.
- `WorkflowTransition.fromStatusRecord` and `WorkflowTransition.toStatusRecord` provide metadata references for those enum transitions.

This model lets the backend expose status metadata and display labels without moving ticket behavior away from the existing `TicketStatus` enum.

## Decision

`tickets.status` remains the behavior source for the current product phase.

`tickets.status_id` and `Ticket.statusRecord` are metadata/display support. They may describe the effective status label and metadata, but they must not independently change ticket behavior.

Custom status assignment through workflow execution remains blocked. A workflow transition may carry metadata records, but current generic execution only supports system workflow statuses whose `status_key` exactly matches the enum target status.

## Rationale

Fixed workflow actions still have behavior that is larger than a status update:

- pick ticket assigns ownership
- start work updates ownership and operational state
- complete ticket enforces authorization rules, then writes completion fields
- cancel ticket enforces admin authorization, then writes cancellation fields

Generic execution intentionally avoids these fixed-action side effects. It also blocks protected fixed action keys, protected transitions, terminal transitions, and target statuses that require unsupported business side effects.

Allowing metadata-only mutation of `Ticket.statusRecord` would create split-brain ticket state: the displayed workflow status could diverge from the enum behavior used by fixed workflows, filters, available-actions, dynamic actions, reports, and authorization checks.

## Current Execution Boundary

Current generic execution must continue to require supported system status metadata:

- status metadata exists
- metadata is active
- metadata is system and protected
- metadata `behaviorBucket` equals the expected enum `TicketStatus`
- metadata `statusKey` equals the expected enum name

If the metadata target key differs from the enum target, custom status assignment is unsupported and remains blocked.

## Future Custom Status Validation Boundary

Future custom status executability checks should be non-mutating and should validate at least:

- target `WorkflowStatus` exists
- target metadata is active
- target has a non-null `behaviorBucket`
- `behaviorBucket` maps to supported `TicketStatus` behavior
- unsafe terminal targets are rejected unless explicitly allowed by a dedicated workflow path
- system/protected boundaries are respected so generic custom execution cannot impersonate fixed workflow behavior
- transition metadata is active before any execution path exposes or runs it

If custom status assignment is approved later, generic execution should update both:

- `tickets.status` to the target status record's `behaviorBucket`
- `tickets.status_id` to the target `WorkflowStatus`

Metadata-only status mutation must remain disallowed unless a later ADR explicitly replaces the enum behavior source.

## Compatibility

This ADR does not approve or implement:

- custom status assignment
- metadata-only ticket status mutation
- fixed workflow replacement
- generic execution into custom statuses
- frontend changes
- available-actions or dynamicActions behavior changes

Existing fixed workflows, generic enum execution, workflow history reads, and Ticket Detail display behavior should remain unchanged.
