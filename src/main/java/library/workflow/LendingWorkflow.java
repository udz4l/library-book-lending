package library.workflow;

public class LendingWorkflow {
    private String id;
    private String bookId;
    private String readerId;
    private String reservationId;
    private String loanId;
    private String confirmationId;
    private WorkflowStatus status;

    // Jackson uses this constructor when loading a workflow from JSON.
    public LendingWorkflow() {
    }

    public LendingWorkflow(String id, String bookId, String readerId) {
        this.id = id;
        this.bookId = bookId;
        this.readerId = readerId;
        this.status = WorkflowStatus.STARTED;
    }

    public void reservationCreated(String reservationId) {
        requireStatus(WorkflowStatus.STARTED);
        this.reservationId = reservationId;
        this.status = WorkflowStatus.PENDING_B;
    }

    public void loanCreated(String loanId) {
        requireStatus(WorkflowStatus.PENDING_B);
        this.loanId = loanId;
        this.status = WorkflowStatus.PENDING_C;
    }

    public void confirmationCreated(String confirmationId) {
        requireStatus(WorkflowStatus.PENDING_C);
        this.confirmationId = confirmationId;
    }

    public void complete() {
        requireStatus(WorkflowStatus.PENDING_C);
        this.status = WorkflowStatus.COMPLETED;
    }

    public void compensate() {
        if (status == WorkflowStatus.COMPLETED
                || status == WorkflowStatus.COMPENSATED) {
            throw new IllegalStateException(
                    "This workflow cannot be compensated.");
        }
        this.status = WorkflowStatus.COMPENSATED;
    }

    private void requireStatus(WorkflowStatus expected) {
        if (status != expected) {
            throw new IllegalStateException(
                    "Expected workflow status " + expected
                            + " but was " + status);
        }
    }

    public String getId() { return id; }
    public String getBookId() { return bookId; }
    public String getReaderId() { return readerId; }
    public String getReservationId() { return reservationId; }
    public String getLoanId() { return loanId; }
    public String getConfirmationId() { return confirmationId; }
    public WorkflowStatus getStatus() { return status; }

    public void setId(String id) { this.id = id; }
    public void setBookId(String bookId) { this.bookId = bookId; }
    public void setReaderId(String readerId) { this.readerId = readerId; }
    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }
    public void setLoanId(String loanId) { this.loanId = loanId; }
    public void setConfirmationId(String confirmationId) {
        this.confirmationId = confirmationId;
    }
    public void setStatus(WorkflowStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return String.format(
                "LendingWorkflow{id='%s', bookId='%s', readerId='%s', "
                        + "reservationId='%s', loanId='%s', confirmationId='%s', status=%s}",
                id, bookId, readerId, reservationId, loanId, confirmationId, status
        );
    }
}