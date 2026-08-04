pragma solidity 0.4.25;

/**
 * @title InternshipRecord
 * @notice On-chain storage for daily internship records with mentor review
 */
contract InternshipRecord {

    uint constant PENDING = 0;
    uint constant APPROVED = 1;
    uint constant REJECTED = 2;

    struct DailyRecord {
        uint256 recordId;
        uint256 internshipId;
        uint256 studentId;
        bytes32 recordDateHash;
        bytes32 contentHash;
        address studentAddress;
        address mentorAddress;
        uint256 timestamp;
        uint status;
        bool mentorApproved;
    }

    mapping(uint256 => DailyRecord[]) records;
    mapping(uint256 => uint256) recordCountByInternship;
    uint256 totalRecordCount = 0;

    event RecordCreated(uint256 indexed recordId, uint256 internshipId, bytes32 contentHash);
    event RecordApproved(uint256 indexed recordId, address mentorAddress);
    event RecordRejected(uint256 indexed recordId, address mentorAddress);

    // ---------- SafeMath inline for Solidity <0.8.0 ----------
    function safeAdd(uint256 a, uint256 b) internal pure returns (uint256) {
        uint256 c = a + b;
        require(c >= a, "SafeMath: addition overflow");
        return c;
    }

    function createRecord(
        uint256 internshipId,
        uint256 studentId,
        bytes32 recordDateHash,
        bytes32 contentHash,
        address studentAddress,
        address mentorAddress
    ) public returns (uint256) {
        uint256 recordId = totalRecordCount;
        totalRecordCount = safeAdd(totalRecordCount, 1);

        records[internshipId].push(DailyRecord({
            recordId: recordId,
            internshipId: internshipId,
            studentId: studentId,
            recordDateHash: recordDateHash,
            contentHash: contentHash,
            studentAddress: studentAddress,
            mentorAddress: mentorAddress,
            timestamp: block.timestamp,
            status: PENDING,
            mentorApproved: false
        }));

        recordCountByInternship[internshipId] = safeAdd(recordCountByInternship[internshipId], 1);
        emit RecordCreated(recordId, internshipId, contentHash);
        return recordId;
    }

    function approveRecord(uint256 internshipId, uint256 recordIndex) public {
        require(recordIndex < records[internshipId].length, "Record not found");
        DailyRecord storage record = records[internshipId][recordIndex];
        require(msg.sender == record.mentorAddress, "Only mentor can approve");
        require(!record.mentorApproved, "Already approved");
        require(record.status == PENDING, "Record already processed");

        record.mentorApproved = true;
        record.status = APPROVED;
        emit RecordApproved(record.recordId, msg.sender);
    }

    function rejectRecord(uint256 internshipId, uint256 recordIndex) public {
        require(recordIndex < records[internshipId].length, "Record not found");
        DailyRecord storage record = records[internshipId][recordIndex];
        require(msg.sender == record.mentorAddress, "Only mentor can reject");
        require(record.status == PENDING, "Record already processed");

        record.status = REJECTED;
        emit RecordRejected(record.recordId, msg.sender);
    }

    function verifyRecord(uint256 internshipId, uint256 recordIndex, bytes32 contentHash) public view returns (bool) {
        if (recordIndex >= records[internshipId].length) {
            return false;
        }
        DailyRecord storage record = records[internshipId][recordIndex];
        return record.contentHash == contentHash && record.mentorApproved && record.status == APPROVED;
    }

    function getRecordInfo(uint256 internshipId, uint256 recordIndex) public view returns (
        uint256 recordId,
        uint256 studentId,
        bytes32 recordDateHash,
        bytes32 contentHash,
        uint256 timestamp,
        uint status,
        bool mentorApproved
    ) {
        require(recordIndex < records[internshipId].length, "Record not found");
        DailyRecord storage record = records[internshipId][recordIndex];
        return (
            record.recordId,
            record.studentId,
            record.recordDateHash,
            record.contentHash,
            record.timestamp,
            record.status,
            record.mentorApproved
        );
    }

    function getRecordStatus(uint256 internshipId, uint256 recordIndex) public view returns (uint) {
        require(recordIndex < records[internshipId].length, "Record not found");
        return records[internshipId][recordIndex].status;
    }

    function getRecordCount(uint256 internshipId) public view returns (uint256) {
        return records[internshipId].length;
    }

    function isRecordExist(uint256 internshipId, uint256 recordIndex) public view returns (bool) {
        return recordIndex < records[internshipId].length;
    }

    function getTotalRecordCount() public view returns (uint256) {
        return totalRecordCount;
    }
}
