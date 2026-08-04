pragma solidity 0.4.25;

/**
 * @title InternshipCertificate
 * @notice On-chain storage for internship certificates with school/enterprise dual approval
 */
contract InternshipCertificate {

    uint constant PENDING = 0;
    uint constant SCHOOL_APPROVED = 1;
    uint constant ENTERPRISE_APPROVED = 2;
    uint constant VERIFIED = 3;

    struct Certificate {
        uint256 certificateId;
        uint256 internshipId;
        uint256 studentId;
        bytes32 contentHash;
        address schoolAddress;
        address enterpriseAddress;
        uint256 timestamp;
        uint status;
        bool schoolApproved;
        bool enterpriseApproved;
    }

    mapping(uint256 => Certificate) certificates;
    mapping(uint256 => bool) certificateExists;
    uint256 certificateCount = 0;

    event CertificateCreated(uint256 indexed certificateId, uint256 internshipId, bytes32 contentHash);
    event SchoolApproved(uint256 indexed certificateId, address schoolAddress);
    event EnterpriseApproved(uint256 indexed certificateId, address enterpriseAddress);
    event CertificateVerified(uint256 indexed certificateId);

    // ---------- SafeMath inline for Solidity <0.8.0 ----------
    function safeAdd(uint256 a, uint256 b) internal pure returns (uint256) {
        uint256 c = a + b;
        require(c >= a, "SafeMath: addition overflow");
        return c;
    }

    function createCertificate(
        uint256 internshipId,
        uint256 studentId,
        bytes32 contentHash,
        address schoolAddress,
        address enterpriseAddress
    ) public returns (uint256) {
        uint256 certificateId = certificateCount;
        certificateCount = safeAdd(certificateCount, 1);

        certificates[certificateId] = Certificate({
            certificateId: certificateId,
            internshipId: internshipId,
            studentId: studentId,
            contentHash: contentHash,
            schoolAddress: schoolAddress,
            enterpriseAddress: enterpriseAddress,
            timestamp: block.timestamp,
            status: PENDING,
            schoolApproved: false,
            enterpriseApproved: false
        });

        certificateExists[certificateId] = true;
        emit CertificateCreated(certificateId, internshipId, contentHash);
        return certificateId;
    }

    function approveBySchool(uint256 certificateId) public {
        require(certificateExists[certificateId], "Certificate not found");
        Certificate storage cert = certificates[certificateId];
        require(msg.sender == cert.schoolAddress, "Only school can approve");
        require(!cert.schoolApproved, "Already approved by school");

        cert.schoolApproved = true;
        if (cert.schoolApproved && cert.enterpriseApproved) {
            cert.status = VERIFIED;
            emit CertificateVerified(certificateId);
        } else {
            cert.status = SCHOOL_APPROVED;
        }
        emit SchoolApproved(certificateId, msg.sender);
    }

    function approveByEnterprise(uint256 certificateId) public {
        require(certificateExists[certificateId], "Certificate not found");
        Certificate storage cert = certificates[certificateId];
        require(msg.sender == cert.enterpriseAddress, "Only enterprise can approve");
        require(!cert.enterpriseApproved, "Already approved by enterprise");

        cert.enterpriseApproved = true;
        if (cert.schoolApproved && cert.enterpriseApproved) {
            cert.status = VERIFIED;
            emit CertificateVerified(certificateId);
        } else {
            cert.status = ENTERPRISE_APPROVED;
        }
        emit EnterpriseApproved(certificateId, msg.sender);
    }

    function verifyCertificate(uint256 certificateId, bytes32 contentHash) public view returns (bool) {
        if (!certificateExists[certificateId]) {
            return false;
        }
        Certificate storage cert = certificates[certificateId];
        return cert.contentHash == contentHash && cert.schoolApproved && cert.enterpriseApproved && cert.status == VERIFIED;
    }

    function getCertificateInfo(uint256 certificateId) public view returns (
        uint256 internshipId,
        uint256 studentId,
        bytes32 contentHash,
        uint256 timestamp,
        uint status,
        bool schoolApproved,
        bool enterpriseApproved
    ) {
        require(certificateExists[certificateId], "Certificate not found");
        Certificate storage cert = certificates[certificateId];
        return (
            cert.internshipId,
            cert.studentId,
            cert.contentHash,
            cert.timestamp,
            cert.status,
            cert.schoolApproved,
            cert.enterpriseApproved
        );
    }

    function getCertificateStatus(uint256 certificateId) public view returns (uint) {
        require(certificateExists[certificateId], "Certificate not found");
        return certificates[certificateId].status;
    }

    function isCertificateExist(uint256 certificateId) public view returns (bool) {
        return certificateExists[certificateId];
    }

    // NOTE: Returns the NEXT certificateId to be assigned (not count of created certificates)
    function getCertificateCount() public view returns (uint256) {
        return certificateCount;
    }
}
