package org.example;

public class UpdateDeniedMessage extends Message {
    private final int reqNo;
    private final String name;
    private final String reason;

        public UpdateDeniedMessage(int reqNo, String name, String reason) {
            super(Code.CONTACT_DENIED);

            this.reqNo = reqNo;
            this.name = name;
            this.reason = reason;
        }

    public int getReqNo() {
        return reqNo;
    }
        public String getName() {
            return name;
        }

        public String getReason() {
            return reason;
        }

    }
