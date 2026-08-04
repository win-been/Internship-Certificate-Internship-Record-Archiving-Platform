package com.internship.controller;

import com.internship.entity.*;
import com.internship.repository.*;
import com.internship.service.BlockchainService;
import com.internship.service.UserChainArchiveService;
import com.internship.util.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/data")
public class DataController {
    private static final Logger log = LoggerFactory.getLogger(DataController.class);
    private static final String IDENTITY_APPROVAL = "IDENTITY_VERIFICATION";
    private static final String ENTERPRISE_QUALIFICATION = "ENTERPRISE_QUALIFICATION";
    private final JobRepository jobRepo;
    private final ApplicationRepository appRepo;
    private final AssessmentRepository assessRepo;
    private final DisputeRepository disputeRepo;
    private final ArchiveRepository archiveRepo;
    private final InternshipRepository internshipRepo;
    private final ReportRepository reportRepo;
    private final NoticeRepository noticeRepo;
    private final PlatformMessageRepository messageRepo;
    private final ApprovalRepository approvalRepo;
    private final UserRepository userRepo;
    private final BlockchainService blockchainService;
    private final UserChainArchiveService userChainArchiveService;
    private final PasswordEncoder passwordEncoder;

    public DataController(JobRepository j, ApplicationRepository a, AssessmentRepository as,
                          DisputeRepository d, ArchiveRepository ar, InternshipRepository ir,
                          ReportRepository rr, NoticeRepository nr, PlatformMessageRepository mr,
                          ApprovalRepository apr, UserRepository u, BlockchainService bs,
                          UserChainArchiveService uca, PasswordEncoder pe) {
        this.jobRepo = j; this.appRepo = a; this.assessRepo = as;
        this.disputeRepo = d; this.archiveRepo = ar; this.internshipRepo = ir;
        this.reportRepo = rr; this.noticeRepo = nr; this.messageRepo = mr;
        this.approvalRepo = apr; this.userRepo = u;
        this.blockchainService = bs;
        this.userChainArchiveService = uca;
        this.passwordEncoder = pe;
    }

    // ===== Jobs =====
    @GetMapping("/jobs")
    public List<Map<String,Object>> getJobs(@RequestParam(required=false) Long companyId) {
        List<Job> jobs = companyId != null ? jobRepo.findByCompanyId(companyId) : jobRepo.findAll();
        return jobs.stream()
                .filter(j -> companyId != null || isApprovedEnterprise(j.getCompanyId()))
                .map(this::mapJob)
                .collect(Collectors.toList());
    }

    @PostMapping("/jobs")
    public Map<String,Object> addJob(Authentication authentication, @RequestBody Map<String,Object> data) {
        String title = asString(data.get("title"));
        Long companyId = asLong(data.get("companyId"));
        if (title == null) throw new RuntimeException("Please enter job title");
        if (companyId == null) throw new RuntimeException("Company is required");
        requireEnterpriseActor(authentication, companyId);
        User enterprise = getApprovedEnterprise(companyId);

        Job job = new Job();
        job.setTitle(title);
        job.setCompanyId(companyId);
        job.setCompany(firstText(enterprise.getOrganizationName(), enterprise.getRealName(), data.get("company")));
        job.setLocation(asString(data.get("location")));
        job.setSalary(asString(data.get("salary")));
        job.setType(asString(data.get("type")));
        job.setDescription(asString(data.get("description")));
        job.setStatus(data.get("status") != null ? asString(data.get("status")) : "OPEN");
        job.setCount(data.get("count") != null ? Integer.valueOf(data.get("count").toString()) : 0);
        Job saved = jobRepo.save(job);
        Map<String,Object> result = mapJob(saved);
        Archive archive = anchorOnChain("宀椾綅鍙戝竷", "宀椾綅:" + saved.getTitle(),
                "job:" + saved.getId() + "|" + saved.getTitle() + "|" + saved.getCompany() + "|" + saved.getStatus(),
                null, saved.getCompanyId(), null, "job:" + saved.getId());
        putChainResult(result, archive);
        return result;
    }

    @PutMapping("/jobs/{id}")
    public Map<String,Object> updateJob(Authentication authentication, @PathVariable Long id, @RequestBody Map<String,Object> data) {
        Job j = jobRepo.findById(id).orElseThrow(() -> new RuntimeException("not found"));
        requireEnterpriseActor(authentication, j.getCompanyId());
        if (data.get("title") != null) j.setTitle((String)data.get("title"));
        if (data.get("companyId") != null && !Objects.equals(asLong(data.get("companyId")), j.getCompanyId())) {
            throw new RuntimeException("Job company cannot be changed while editing");
        }
        if (data.get("location") != null) j.setLocation((String)data.get("location"));
        if (data.get("salary") != null) j.setSalary((String)data.get("salary"));
        if (data.get("type") != null) j.setType((String)data.get("type"));
        if (data.get("description") != null) j.setDescription((String)data.get("description"));
        if (data.get("status") != null) j.setStatus((String)data.get("status"));
        if (data.get("count") != null) j.setCount(Integer.valueOf(data.get("count").toString()));
        getApprovedEnterprise(j.getCompanyId());
        j.setCompany(firstText(enterpriseDisplayName(j.getCompanyId()), j.getCompany()));
        Job saved = jobRepo.save(j);
        Map<String,Object> result = mapJob(saved);
        Archive archive = anchorOnChain("宀椾綅鏇存柊", "宀椾綅:" + saved.getTitle(),
                "job-update:" + saved.getId() + "|" + saved.getStatus(),
                null, saved.getCompanyId(), null, "job:" + saved.getId());
        putChainResult(result, archive);
        return result;
    }

    @DeleteMapping("/jobs/{id}")
    public Map<String,String> deleteJob(Authentication authentication, @PathVariable Long id) {
        Job job = jobRepo.findById(id).orElse(null);
        if (job != null) requireEnterpriseActor(authentication, job.getCompanyId());
        jobRepo.deleteById(id);
        anchorOnChain("宀椾綅鍒犻櫎", "宀椾綅#" + id, "job-delete:" + id,
                null, job != null ? job.getCompanyId() : null, null, "job:" + id);
        return Map.of("status","ok");
    }

    // ===== Applications =====
    @GetMapping("/applications")
    public List<Map<String,Object>> getApplications(Authentication authentication,
                                                     @RequestParam(required=false) Long studentId,
                                                     @RequestParam(required=false) Long companyId,
                                                     @RequestParam(required=false) Long schoolId) {
        List<Application> apps;
        if (studentId != null) apps = appRepo.findByStudentId(studentId);
        else if (companyId != null) apps = appRepo.findByCompanyId(companyId);
        else apps = appRepo.findAll();
        Long scopedSchoolId = resolveViewerSchoolId(authentication, schoolId);
        if (scopedSchoolId != null) {
            apps = apps.stream()
                    .filter(app -> applicationBelongsToSchool(app, scopedSchoolId))
                    .collect(Collectors.toList());
        }
        return apps.stream()
                .filter(app -> applicationVisibleTo(authentication, app))
                .map(this::mapApplication)
                .collect(Collectors.toList());
    }

    @PostMapping("/applications")
    public Map<String,Object> addApplication(Authentication authentication, @RequestBody Map<String,Object> data) {
        Long studentId = asLong(data.get("studentId"));
        Long jobId = asLong(data.get("jobId"));
        if (studentId == null) throw new RuntimeException("Student is required");
        if (jobId == null) throw new RuntimeException("Job is required");
        requireStudentActor(authentication, studentId);
        User student = getApprovedStudent(studentId);
        Job job = jobRepo.findById(jobId).orElseThrow(() -> new RuntimeException("Job does not exist"));
        if (!"OPEN".equals(job.getStatus())) throw new RuntimeException("Job is not open");
        getApprovedEnterprise(job.getCompanyId());
        ensureStudentCanApply(studentId, null);
        boolean duplicated = appRepo.findByStudentId(studentId).stream()
                .anyMatch(a -> Objects.equals(a.getJobId(), jobId) && !"rejected".equals(a.getStatus()));
        if (duplicated) throw new RuntimeException("Already applied to this job");

        Application app = new Application();
        app.setStudentId(studentId);
        app.setName(firstDisplayText(student.getRealName(), data.get("name"), student.getUsername()));
        app.setJobId(jobId);
        app.setJobTitle(jobDisplayTitle(job));
        app.setSchool(studentSchoolName(student, data.get("school")));
        app.setMajor(studentMajor(student, data.get("major")));
        app.setApplyDate((String)data.get("applyDate"));
        if (app.getApplyDate() == null || app.getApplyDate().isBlank()) app.setApplyDate(LocalDate.now().toString());
        app.setStatus("pending");
        app.setCompanyId(job.getCompanyId());
        Application saved = appRepo.save(app);
        refreshJobCount(jobId);
        Archive archive = anchorOnChain("APPLICATION_SUBMIT", "Student:" + saved.getName() + "->Job:" + saved.getJobTitle(),
                "app:" + saved.getId() + "|" + saved.getStudentId() + "|" + saved.getJobId(),
                saved.getStudentId(), saved.getCompanyId(), null, "application:" + saved.getId());
        Map<String,Object> result = mapApplication(saved);
        putChainResult(result, archive);
        return result;
    }

    @PutMapping("/applications/{id}")
    public Map<String,Object> updateApp(Authentication authentication, @PathVariable Long id, @RequestBody Map<String,Object> data) {
        Application a = appRepo.findById(id).orElseThrow(() -> new RuntimeException("not found"));
        Job job = jobRepo.findById(a.getJobId()).orElseThrow(() -> new RuntimeException("Job does not exist"));
        requireEnterpriseActor(authentication, job.getCompanyId());
        getApprovedEnterprise(job.getCompanyId());
        a.setCompanyId(job.getCompanyId());
        a.setJobTitle(job.getTitle());
        String status = asString(data.get("status"));
        boolean accepting = "accepted".equals(status) && !"accepted".equals(a.getStatus());
        if (accepting) {
            ensureStudentCanApply(a.getStudentId(), a.getId());
            appRepo.findByStudentId(a.getStudentId()).stream()
                    .filter(other -> !Objects.equals(other.getId(), a.getId()))
                    .filter(other -> "pending".equals(other.getStatus()))
                    .forEach(other -> {
                        other.setStatus("rejected");
                        appRepo.save(other);
                        refreshJobCount(other.getJobId());
                    });
        }
        if (status != null) a.setStatus(status);
        Application saved = appRepo.save(a);
        refreshJobCount(saved.getJobId());
        if (accepting) createInternshipForAcceptedApplication(saved, data);
        Archive archive = anchorOnChain("褰曠敤澶勭悊", "瀛︾敓:" + saved.getName() + "-" + saved.getStatus(),
                "app-update:" + saved.getId() + "|" + saved.getStatus(),
                saved.getStudentId(), saved.getCompanyId(), null, "application:" + saved.getId());
        Map<String,Object> result = mapApplication(saved);
        putChainResult(result, archive);
        return result;
    }

    @DeleteMapping("/applications/{id}")
    public Map<String,String> deleteApp(Authentication authentication, @PathVariable Long id) {
        Application app = appRepo.findById(id).orElseThrow(() -> new RuntimeException("not found"));
        requireEnterpriseActor(authentication, app.getCompanyId());
        appRepo.deleteById(id);
        return Map.of("status","ok");
    }

    // ===== Assessments =====
    @GetMapping("/assessments")
    public List<Map<String,Object>> getAssessments(Authentication authentication,
                                                    @RequestParam(required=false) Long studentId,
                                                    @RequestParam(required=false) Long companyId,
                                                    @RequestParam(required=false) Long internshipId,
                                                    @RequestParam(required=false) Long schoolId) {
        List<Assessment> list;
        if (internshipId != null) list = assessRepo.findByInternshipId(internshipId);
        else if (studentId != null) list = assessRepo.findByStudentId(studentId);
        else if (companyId != null) list = assessRepo.findByCompanyId(companyId);
        else list = assessRepo.findAll();
        Long scopedSchoolId = resolveViewerSchoolId(authentication, schoolId);
        if (scopedSchoolId != null) {
            list = list.stream()
                    .filter(assessment -> assessmentBelongsToSchool(assessment, scopedSchoolId))
                    .collect(Collectors.toList());
        }
        return list.stream()
                .filter(assessment -> assessmentVisibleTo(authentication, assessment))
                .map(this::mapAssessment)
                .collect(Collectors.toList());
    }

    @PostMapping("/assessments")
    public Map<String,Object> addAssessment(Authentication authentication, @RequestBody Map<String,Object> data) {
        Long studentId = asLong(data.get("studentId"));
        Long companyId = firstLong(data.get("companyId"), data.get("enterpriseId"));
        String month = asString(data.get("month"));
        if (month == null) throw new RuntimeException("Assessment month is required");
        InternshipInfo internship = requireInternshipForAssessment(studentId, companyId, asLong(data.get("internshipId")));
        requireEnterpriseActor(authentication, internship.getEnterpriseId());
        User student = getApprovedStudent(internship.getStudentId());
        Assessment a = assessRepo.findFirstByInternshipIdAndMonth(internship.getId(), month).orElseGet(Assessment::new);
        a.setInternshipId(internship.getId());
        a.setStudent(firstText(student.getRealName(), student.getUsername()));
        a.setStudentId(student.getId());
        a.setCompanyId(internship.getEnterpriseId());
        a.setMonth(month);
        a.setAttendance(normalizeAttendance(data.get("attendance")));
        a.setScore(data.get("score") != null ? Integer.valueOf(data.get("score").toString()) : null);
        a.setComment(asString(data.get("comment")));
        a.setStatus("COMPLETED");
        Assessment saved = assessRepo.save(a);
        Archive archive = anchorOnChain("鑰冩牳鎻愪氦", "瀛︾敓:" + saved.getStudent() + "-" + saved.getMonth(),
                "assessment:" + saved.getId() + "|" + saved.getInternshipId() + "|" + saved.getScore(),
                saved.getStudentId(), saved.getCompanyId(), saved.getInternshipId(), "assessment:" + saved.getId());
        Map<String,Object> result = mapAssessment(saved);
        putChainResult(result, archive);
        return result;
    }

    // ===== Disputes =====
    @GetMapping("/disputes")
    public List<Map<String,Object>> getDisputes(Authentication authentication,
                                                @RequestParam(required=false) Long studentId,
                                                @RequestParam(required=false) Long companyId,
                                                @RequestParam(required=false) Long assessmentId) {
        List<Dispute> list;
        if (assessmentId != null) list = disputeRepo.findByAssessmentId(assessmentId);
        else if (studentId != null) list = disputeRepo.findByStudentId(studentId);
        else if (companyId != null) list = disputeRepo.findByCompanyId(companyId);
        else list = disputeRepo.findAll();
        return list.stream()
                .filter(dispute -> disputeVisibleTo(authentication, dispute))
                .map(this::mapDispute)
                .collect(Collectors.toList());
    }

    @PutMapping("/disputes/{id}")
    public Map<String,Object> resolveDispute(Authentication authentication, @PathVariable Long id, @RequestBody Map<String,Object> data) {
        Dispute d = disputeRepo.findById(id).orElseThrow(() -> new RuntimeException("not found"));
        requireEnterpriseActor(authentication, d.getCompanyId());
        d.setStatus((String)data.get("status"));
        d.setOpinion((String)data.get("opinion"));
        d.setResolvedDate((String)data.get("resolvedDate"));
        Dispute saved = disputeRepo.save(d);
        Long internshipId = saved.getAssessmentId() != null
                ? assessRepo.findById(saved.getAssessmentId()).map(Assessment::getInternshipId).orElse(null)
                : null;
        Archive archive = anchorOnChain("绾犵悍澶勭悊", "瀛︾敓:" + saved.getStudent() + "-" + saved.getStatus(),
                "dispute:" + saved.getId() + "|" + saved.getAssessmentId() + "|" + saved.getStatus(),
                saved.getStudentId(), saved.getCompanyId(), internshipId, "dispute:" + saved.getId());
        Map<String,Object> result = mapDispute(saved);
        putChainResult(result, archive);
        return result;
    }

    @PostMapping("/disputes")
    public Map<String,Object> addDispute(Authentication authentication, @RequestBody Map<String,Object> data) {
        Long assessmentId = asLong(data.get("assessmentId"));
        Long studentId = asLong(data.get("studentId"));
        if (assessmentId == null) throw new RuntimeException("Assessment is required");
        Assessment assessment = assessRepo.findById(assessmentId).orElseThrow(() -> new RuntimeException("Assessment does not exist"));
        requireStudentActor(authentication, assessment.getStudentId());
        if (studentId != null && !Objects.equals(studentId, assessment.getStudentId())) {
            throw new RuntimeException("Dispute student does not match assessment");
        }
        boolean duplicated = disputeRepo.findByAssessmentId(assessmentId).stream()
                .anyMatch(d -> !"RESOLVED".equals(d.getStatus()));
        if (duplicated) throw new RuntimeException("This assessment already has a pending dispute");
        Dispute d = new Dispute();
        d.setAssessmentId(assessmentId);
        d.setStudentId(assessment.getStudentId());
        d.setCompanyId(assessment.getCompanyId());
        d.setStudent(assessment.getStudent());
        d.setReason(firstDisplayText(data.get("reason"), disputeReasonFallback(assessment)));
        d.setDate(firstText(data.get("date"), LocalDate.now().toString()));
        d.setStatus("PENDING");
        Dispute saved = disputeRepo.save(d);
        Archive archive = anchorOnChain("绾犵悍鍙戣捣", "瀛︾敓:" + saved.getStudent(),
                "dispute-new:" + saved.getId() + "|" + saved.getAssessmentId(),
                saved.getStudentId(), saved.getCompanyId(), assessment.getInternshipId(), "dispute:" + saved.getId());
        Map<String,Object> result = mapDispute(saved);
        putChainResult(result, archive);
        return result;
    }

    // ===== Archives =====
    @GetMapping("/archives")
    public List<Map<String,Object>> getArchives(Authentication authentication,
                                                 @RequestParam(required=false) Long studentId,
                                                 @RequestParam(required=false) Long companyId,
                                                 @RequestParam(required=false) Long internshipId) {
        List<Archive> list;
        if (internshipId != null) list = archiveRepo.findAll().stream()
                .filter(a -> Objects.equals(a.getInternshipId(), internshipId))
                .collect(Collectors.toList());
        else if (studentId != null) list = archiveRepo.findByStudentId(studentId);
        else if (companyId != null) list = archiveRepo.findByCompanyId(companyId);
        else list = archiveRepo.findAll();
        return list.stream()
                .map(this::backfillArchiveLinks)
                .filter(archive -> archiveVisibleTo(authentication, archive))
                .map(this::mapArchive)
                .collect(Collectors.toList());
    }

    @PostMapping("/archives")
    public Map<String,Object> addArchive(Authentication authentication, @RequestBody Map<String,Object> data) {
        Archive a = new Archive();
        a.setType((String)data.get("type"));
        a.setName((String)data.get("name"));
        a.setHash((String)data.get("hash"));
        a.setTime((String)data.get("time"));
        a.setBlock(data.get("block") != null ? Long.valueOf(data.get("block").toString()) : null);
        a.setStudentId(data.get("studentId") != null ? Long.valueOf(data.get("studentId").toString()) : null);
        a.setCompanyId(data.get("companyId") != null ? Long.valueOf(data.get("companyId").toString()) : null);
        a.setInternshipId(data.get("internshipId") != null ? Long.valueOf(data.get("internshipId").toString()) : null);
        a.setSourceId(asString(data.get("sourceId")));
        enforceArchiveWritable(authentication, a);
        Archive saved = saveArchiveWithChainState(a, "archive:" + a.getType() + ":" + a.getName() + ":" + a.getHash());
        return mapArchive(saved);
    }

    // ===== Blockchain Health =====
    @GetMapping("/blockchain/health")
    public Map<String,Object> checkBlockchainHealth() {
        return blockchainService.checkHealth();
    }

    @PostMapping("/blockchain/deploy-contracts")
    public Map<String,Object> deployContracts(Authentication authentication) {
        requirePlatformAdmin(authentication);
        return blockchainService.deployContracts();
    }

    // ===== Internships =====
    @GetMapping("/internships")
    public List<Map<String,Object>> getInternships(Authentication authentication,
                                                    @RequestParam(required=false) Long studentId,
                                                    @RequestParam(required=false) Long enterpriseId,
                                                    @RequestParam(required=false) Long schoolId) {
        List<InternshipInfo> list;
        Long scopedSchoolId = resolveViewerSchoolId(authentication, schoolId);
        if (studentId != null) list = internshipRepo.findByStudentId(studentId);
        else if (enterpriseId != null) list = internshipRepo.findByEnterpriseId(enterpriseId);
        else if (scopedSchoolId != null) list = internshipRepo.findAll();
        else list = internshipRepo.findAll();
        if (scopedSchoolId != null) {
            list = list.stream()
                    .filter(i -> internshipBelongsToSchool(i, scopedSchoolId))
                    .collect(Collectors.toList());
        }
        return list.stream()
                .filter(internship -> internshipVisibleTo(authentication, internship))
                .map(this::mapInternship)
                .collect(Collectors.toList());
    }

    @PostMapping("/internships")
    public Map<String,Object> addInternship(Authentication authentication, @RequestBody Map<String,Object> data) {
        Long studentId = asLong(data.get("studentId"));
        Long enterpriseId = firstLong(data.get("enterpriseId"), data.get("companyId"));
        requireEnterpriseActor(authentication, enterpriseId);
        User student = getApprovedStudent(studentId);
        User enterprise = getApprovedEnterprise(enterpriseId);
        Application acceptedApplication = requireAcceptedApplication(student.getId(), enterprise.getId());
        internshipRepo.findByStudentId(student.getId()).stream()
                .filter(prev -> "ACTIVE".equals(prev.getStatus()))
                .findFirst()
                .ifPresent(prev -> {
                    throw new RuntimeException("璇ュ鐢熷凡鏈夊湪宀楀疄涔狅紝涓嶈兘閲嶅鍒涘缓瀹炰範澶囨");
                });

        InternshipInfo i = new InternshipInfo();
        i.setStudentId(student.getId());
        i.setStudentName(firstText(student.getRealName(), student.getUsername()));
        i.setStudentIdCard(student.getIdCard());
        i.setEnterpriseId(enterprise.getId());
        i.setEnterpriseName(firstText(enterprise.getOrganizationName(), enterprise.getRealName(), enterprise.getUsername()));
        i.setEnterpriseCode(enterprise.getOrganizationCode());
        i.setSchoolId(firstLong(data.get("schoolId"), student.getSchoolId()));
        i.setSchoolName(studentSchoolName(student, data.get("schoolName"), acceptedApplication.getSchool()));
        Job acceptedJob = resolveJobForApplication(acceptedApplication);
        i.setPosition(jobDisplayTitle(acceptedJob, data.get("position"), acceptedApplication.getJobTitle()));
        i.setStartDate(asDate(data.get("startDate")));
        i.setEndDate(asDate(data.get("endDate")));
        i.setDepartment(asString(data.get("department")));
        i.setMentorName(asString(data.get("mentorName")));
        i.setMentorPhone(asString(data.get("mentorPhone")));
        i.setDescription(asString(data.get("description")));
        i.setStatus(data.get("status") != null ? asString(data.get("status")) : "ACTIVE");
        String contentHash = BlockchainService.calculateHash(
                i.getStudentId() + "|" + i.getEnterpriseId() + "|" + i.getPosition() + "|" + i.getStartDate());
        i.setContentHash(contentHash);
        InternshipInfo saved = internshipRepo.save(i);
        Archive archive = anchorOnChain("\u5b9e\u4e60\u767b\u8bb0", "\u5b66\u751f:" + saved.getStudentName(),
                "internship:" + saved.getId() + ":" + saved.getContentHash(),
                saved.getStudentId(), saved.getEnterpriseId(), saved.getId(), "internship:" + saved.getId());
        if (archive != null) saved.setBlockchainTxHash(archive.getTxHash());
        saved = internshipRepo.save(saved);
        return mapInternship(saved);
    }

    // ===== Reports =====
    @GetMapping("/reports")
    public List<Map<String,Object>> getReports(Authentication authentication,
                                                @RequestParam(required=false) Long studentId,
                                                @RequestParam(required=false) Long enterpriseId,
                                                @RequestParam(required=false) Long internshipId,
                                                @RequestParam(required=false) Long schoolId) {
        List<Report> reports;
        if (studentId != null) reports = reportRepo.findByStudentId(studentId);
        else if (enterpriseId != null) reports = reportRepo.findByEnterpriseId(enterpriseId);
        else if (internshipId != null) reports = reportRepo.findByInternshipId(internshipId);
        else reports = reportRepo.findAll();
        Long scopedSchoolId = resolveViewerSchoolId(authentication, schoolId);
        if (scopedSchoolId != null) {
            reports = reports.stream()
                    .filter(report -> reportBelongsToSchool(report, scopedSchoolId))
                    .collect(Collectors.toList());
        }
        return reports.stream()
                .filter(report -> reportVisibleTo(authentication, report))
                .map(this::mapReport)
                .collect(Collectors.toList());
    }

    @PostMapping("/reports")
    public Map<String,Object> addReport(Authentication authentication, @RequestBody Map<String,Object> data) {
        Long studentId = asLong(data.get("studentId"));
        requireStudentActor(authentication, studentId);
        User student = getApprovedStudent(studentId);
        InternshipInfo internship = requireActiveInternshipForReport(student.getId(), asLong(data.get("internshipId")));
        Report r = new Report();
        r.setInternshipId(internship.getId());
        r.setStudentId(student.getId());
        r.setEnterpriseId(internship.getEnterpriseId());
        r.setStudentName(firstText(student.getRealName(), student.getUsername()));
        r.setEnterpriseName(internship.getEnterpriseName());
        r.setTitle(asString(data.get("title")));
        r.setContent(asString(data.get("content")));
        r.setDate(asString(data.get("date")));
        r.setHours(asDouble(firstText(data.get("hours"), data.get("workHours"))));
        r.setSubmitted(data.get("submitted") == null || Boolean.parseBoolean(data.get("submitted").toString()));
        Report saved = reportRepo.save(r);
        anchorOnChain("\u65e5\u62a5\u63d0\u4ea4", "\u65e5\u62a5:" + saved.getTitle(),
                "report:" + saved.getId() + ":" + saved.getInternshipId() + ":" + saved.getTitle(),
                saved.getStudentId(), saved.getEnterpriseId(), saved.getInternshipId(), "report:" + saved.getId());
        return mapReport(saved);
    }

    // ===== Notices =====
    @GetMapping("/notices")
    public List<Map<String,Object>> getNotices(Authentication authentication) {
        return noticeRepo.findAll().stream()
                .filter(notice -> noticeVisibleTo(authentication, notice))
                .map(this::mapNotice)
                .collect(Collectors.toList());
    }

    @PostMapping("/notices")
    public Map<String,Object> addNotice(Authentication authentication, @RequestBody Map<String,Object> data) {
        Notice n = new Notice();
        User user = currentUser(authentication);
        Long schoolId = "SCHOOL_ADMIN".equals(user.getRole())
                ? resolveWritableSchoolId(authentication, asLong(data.get("schoolId")))
                : asLong(data.get("schoolId"));
        if (!"SCHOOL_ADMIN".equals(user.getRole()) && !"PLATFORM_ADMIN".equals(user.getRole())) {
            throw new RuntimeException("Only school or platform admin can publish notices");
        }
        String title = asString(data.get("title"));
        String content = asString(data.get("content"));
        if (title == null) throw new RuntimeException("璇疯緭鍏ラ€氱煡鏍囬");
        if (content == null) throw new RuntimeException("璇疯緭鍏ラ€氱煡鍐呭");
        n.setTitle(title);
        n.setContent(content);
        n.setFromName(firstText(asString(data.get("from")), user.getOrganizationName(), user.getRealName(), user.getUsername()));
        n.setDate(asString(data.get("date")));
        n.setType(asString(data.get("type")));
        n.setSchoolId(schoolId);
        Notice saved = noticeRepo.save(n);
        anchorOnChain("\u901a\u77e5\u53d1\u5e03", "\u901a\u77e5:" + saved.getTitle(), "notice:" + saved.getId() + ":" + saved.getTitle() + ":" + saved.getContent());
        return mapNotice(saved);
    }

    // ===== School Students (linked to real User data) =====
    @GetMapping("/school-students")
    public List<Map<String,Object>> getSchoolStudents(Authentication authentication,
                                                       @RequestParam(required=false) Long schoolId) {
        List<User> users;
        User viewer = currentUser(authentication);
        if ("PLATFORM_ADMIN".equals(viewer.getRole())) {
            users = schoolId != null ? userRepo.findBySchoolIdAndRole(schoolId, "STUDENT") : userRepo.findByRole("STUDENT");
        } else if ("SCHOOL_ADMIN".equals(viewer.getRole())) {
            Long scopedSchoolId = viewerSchoolId(viewer);
            users = userRepo.findBySchoolIdAndRole(scopedSchoolId, "STUDENT");
        } else if ("STUDENT".equals(viewer.getRole())) {
            users = List.of(viewer);
        } else {
            users = List.of();
        }
        return users.stream().map(u -> {
            InternshipInfo active = internshipRepo.findByStudentId(u.getId()).stream()
                    .filter(i -> "ACTIVE".equals(i.getStatus()))
                    .max(Comparator.comparing(InternshipInfo::getStartDate, Comparator.nullsLast(Comparator.naturalOrder())))
                    .orElse(null);
            Map<String,Object> m = new HashMap<>();
            m.put("studentNo", "S" + u.getId());
            m.put("name", u.getRealName() != null ? u.getRealName() : u.getUsername());
            m.put("idCard", u.getIdCard());
            m.put("schoolName", userOrganizationName(u));
            m.put("major", firstText(userMajor(u), "\u672a\u586b\u5199"));
            m.put("enterprise", active != null ? firstText(active.getEnterpriseName(), enterpriseDisplayName(active.getEnterpriseId()), "-") : "-");
            m.put("position", active != null ? firstText(active.getPosition(), "-") : "-");
            m.put("startDate", active != null && active.getStartDate() != null ? active.getStartDate().toString() : "");
            m.put("schoolId", u.getSchoolId());
            m.put("userId", u.getId());
            m.put("accountStatus", u.getStatus());
            m.put("accountStatusLabel", studentAccountStatusLabel(u));
            m.put("status", active != null ? "ACTIVE" : "INACTIVE");
            m.put("statusLabel", active != null ? "\u5728\u5c97" : "\u79bb\u5c97");
            m.put("statusColor", active != null ? "green" : "orange");
            return m;
        }).collect(Collectors.toList());
    }

    @PostMapping("/school-students")
    public Map<String,Object> addSchoolStudent(Authentication authentication, @RequestBody Map<String,Object> data) {
        User user = new User();
        String studentNo = (String)data.getOrDefault("studentNo", "S" + System.currentTimeMillis());
        Long schoolId = resolveWritableSchoolId(authentication, asLong(data.get("schoolId")));
        String name = asString(data.get("name"));
        String idCard = asString(data.get("idCard"));
        if (name == null) throw new RuntimeException("Please enter student real name");
        if (idCard == null || !idCard.matches("\\d{17}[\\dXx]")) throw new RuntimeException("Please enter a valid 18-digit student ID card");
        boolean duplicated = userRepo.findByRoleAndIdCard("STUDENT", idCard).stream()
                .anyMatch(u -> !Objects.equals(u.getId(), user.getId()));
        if (duplicated) throw new RuntimeException("璇ュ鐢熻韩浠借瘉鍙峰凡瀛樺湪");
        if (userRepo.existsByUsername(studentNo.toLowerCase())) throw new RuntimeException("Student number already exists");
        user.setUsername(studentNo.toLowerCase());
        user.setPassword(passwordEncoder.encode("123456"));
        user.setRealName(name);
        user.setRole("STUDENT");
        user.setSchoolId(schoolId);
        user.setOrganizationName(resolveSchoolName(schoolId, asString(data.get("schoolName"))));
        user.setMajor(asString(data.get("major")));
        user.setIdCard(idCard);
        user.setEnabled(false);
        user.setApproved(false);
        user.setStatus("ROSTER");
        user.setIdentityStatus("UNSUBMITTED");
        User saved = userRepo.save(user);
        userChainArchiveService.anchorUserProfile(saved);
        userChainArchiveService.anchorUserEvent(saved, "USER_STUDENT_ROSTER_CREATE");
        Map<String,Object> result = new HashMap<>();
        result.put("studentNo", "S" + saved.getId());
        result.put("name", saved.getRealName());
        result.put("idCard", saved.getIdCard());
        result.put("major", firstText(saved.getMajor(), ""));
        result.put("enterprise", data.getOrDefault("enterprise", "-"));
        result.put("position", data.getOrDefault("position", "-"));
        result.put("startDate", data.getOrDefault("startDate", ""));
        result.put("schoolId", saved.getSchoolId());
        result.put("userId", saved.getId());
        result.put("accountStatus", saved.getStatus());
        result.put("accountStatusLabel", studentAccountStatusLabel(saved));
        result.put("status", "INACTIVE");
        result.put("statusLabel", "\u79bb\u5c97");
        result.put("statusColor", "orange");
        return result;
    }

    @PutMapping("/school-students/{studentNo}")
    public Map<String,Object> updateSchoolStudent(Authentication authentication, @PathVariable String studentNo, @RequestBody Map<String,Object> data) {
        Long userId = Long.valueOf(studentNo.replace("S", ""));
        User u = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("not found"));
        enforceSchoolStudentWritable(authentication, u);
        if (data.get("name") != null) u.setRealName((String)data.get("name"));
        if (data.get("major") != null) u.setMajor(asString(data.get("major")));
        if (data.get("idCard") != null) {
            String idCard = asString(data.get("idCard"));
            if (idCard == null || !idCard.matches("\\d{17}[\\dXx]")) throw new RuntimeException("Please enter a valid 18-digit student ID card");
            boolean duplicated = userRepo.findByRoleAndIdCard("STUDENT", idCard).stream()
                    .anyMatch(other -> !Objects.equals(other.getId(), u.getId()));
            if (duplicated) throw new RuntimeException("璇ュ鐢熻韩浠借瘉鍙峰凡瀛樺湪");
            u.setIdCard(idCard);
        }
        User saved = userRepo.save(u);
        userChainArchiveService.anchorUserProfile(saved);
        userChainArchiveService.anchorUserEvent(saved, "USER_STUDENT_ROSTER_UPDATE");
        Map<String,Object> result = new HashMap<>();
        result.put("studentNo", "S" + saved.getId());
        result.put("name", saved.getRealName());
        result.put("idCard", saved.getIdCard());
        result.put("schoolName", saved.getOrganizationName());
        result.put("major", saved.getMajor());
        result.put("accountStatus", saved.getStatus());
        result.put("accountStatusLabel", studentAccountStatusLabel(saved));
        return result;
    }

    private String studentAccountStatusLabel(User user) {
        String status = user != null ? user.getStatus() : null;
        if ("ROSTER".equals(status)) return "\u540d\u5355\u672a\u6ce8\u518c";
        if ("APPROVED".equals(status) || Boolean.TRUE.equals(user != null ? user.getApproved() : null)) return "\u5df2\u5f00\u901a";
        if ("PENDING".equals(status)) return "\u5f85\u5ba1\u6838";
        if ("REJECTED".equals(status)) return "\u5df2\u9a73\u56de";
        return "\u540d\u5355\u672a\u6ce8\u518c";
    }

    @PutMapping("/school-students/{studentNo}/status")
    public Map<String,Object> toggleStudentStatus(Authentication authentication, @PathVariable String studentNo, @RequestBody Map<String,Object> data) {
        Long userId = Long.valueOf(studentNo.replace("S", ""));
        User u = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("not found"));
        enforceSchoolStudentWritable(authentication, u);
        String newStatus = asString(data.get("status"));
        if (!"ACTIVE".equals(newStatus) && !"INACTIVE".equals(newStatus)) {
            throw new RuntimeException("Unsupported student internship status");
        }
        List<InternshipInfo> internships = internshipRepo.findByStudentId(u.getId());
        InternshipInfo internship;
        if ("ACTIVE".equals(newStatus)) {
            internship = internships.stream()
                    .filter(i -> "ACTIVE".equals(i.getStatus()))
                    .findFirst()
                    .or(() -> internships.stream()
                            .filter(i -> i.getEnterpriseId() != null)
                            .max(Comparator.comparing(InternshipInfo::getId)))
                    .orElseThrow(() -> new RuntimeException("Cannot mark student active before an internship is created"));
            internship.setStatus("ACTIVE");
        } else {
            internship = internships.stream()
                    .filter(i -> "ACTIVE".equals(i.getStatus()))
                    .findFirst()
                    .orElse(null);
            if (internship != null) {
                internship.setStatus("COMPLETED");
                if (internship.getEndDate() == null) internship.setEndDate(LocalDate.now());
            }
        }
        if (internship != null) {
            InternshipInfo savedInternship = internshipRepo.save(internship);
            anchorOnChain("INTERNSHIP_STATUS_CHANGE", "Student:" + firstText(u.getRealName(), u.getUsername()),
                    "internship-status:" + savedInternship.getId() + "|" + savedInternship.getStatus(),
                    savedInternship.getStudentId(), savedInternship.getEnterpriseId(), savedInternship.getId(),
                    "internship:" + savedInternship.getId());
        }
        String label = "ACTIVE".equals(newStatus) ? "\u5728\u5c97" : "\u79bb\u5c97";
        String color = "ACTIVE".equals(newStatus) ? "green" : "orange";
        return Map.of(
                "studentNo", "S" + u.getId(),
                "status", newStatus,
                "statusLabel", label,
                "statusColor", color,
                "accountStatus", u.getStatus(),
                "accountStatusLabel", studentAccountStatusLabel(u)
        );
    }

    // ===== Enterprise List (linked to real User data) =====
    @GetMapping("/enterprise-list")
    public List<Map<String,Object>> getEnterpriseList(@RequestParam(required=false) Long schoolId) {
        List<User> users = userRepo.findByRole("ENTERPRISE_HR");
        return users.stream().map(this::mapEnterpriseListItem).collect(Collectors.toList());
    }

    @PostMapping("/enterprise-list")
    public Map<String,Object> addEnterpriseRoster(Authentication authentication, @RequestBody Map<String,Object> data) {
        requireSchoolOrPlatformAdmin(authentication);
        String name = asString(data.get("name"));
        String code = asString(data.get("code"));
        if (name == null) throw new RuntimeException("Please enter enterprise name");
        if (code == null) throw new RuntimeException("Please enter enterprise credit code");
        if (!userRepo.findByRoleAndOrganizationCode("ENTERPRISE_HR", code).isEmpty()) {
            throw new RuntimeException("璇ヤ紒涓氫俊鐢ㄤ唬鐮佸凡鍦ㄥ悕鍗曚腑");
        }
        User user = new User();
        user.setUsername(uniqueRosterUsername("enterprise", code));
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setRole("ENTERPRISE_HR");
        user.setOrganizationName(name);
        user.setOrganizationCode(code);
        user.setRealName(firstText(data.get("contact"), name));
        user.setPhone(asString(data.get("phone")));
        user.setEmail(asString(data.get("email")));
        user.setEnabled(false);
        user.setApproved(false);
        user.setStatus("ROSTER");
        User saved = userRepo.save(user);
        userChainArchiveService.anchorUserProfile(saved);
        userChainArchiveService.anchorUserEvent(saved, "USER_ENTERPRISE_ROSTER_CREATE");
        return mapEnterpriseListItem(saved);
    }

    @PutMapping("/enterprise-list/{id}/approve")
    public Map<String,Object> approveEnterprise(Authentication authentication, @PathVariable Long id) {
        requirePlatformAdmin(authentication);
        User u = userRepo.findById(id).orElseThrow(() -> new RuntimeException("not found"));
        u.setApproved(true);
        u.setStatus("APPROVED");
        u.setEnabled(true);
        u.setApprovedAt(LocalDateTime.now());
        User saved = userRepo.save(u);
        saveEnterpriseQualificationDecision(saved, "APPROVED");
        userChainArchiveService.anchorUserProfile(saved);
        userChainArchiveService.anchorUserEvent(saved, "USER_ENTERPRISE_APPROVE");
        anchorOnChain("浼佷笟璧勮川瀹℃牳閫氳繃", "浼佷笟:" + firstText(saved.getOrganizationName(), saved.getUsername()),
                "enterprise-approve:" + saved.getId() + "|" + saved.getStatus(), null, saved.getId());
        return Map.of("id", saved.getId(), "status", "APPROVED", "statusLabel", "\u5df2\u5165\u9a7b");
    }

    @PutMapping("/enterprise-list/{id}/final-approve")
    public Map<String,Object> finalApproveEnterprise(Authentication authentication, @PathVariable Long id) {
        requirePlatformAdmin(authentication);
        User u = userRepo.findById(id).orElseThrow(() -> new RuntimeException("not found"));
        u.setApproved(true);
        u.setStatus("APPROVED");
        u.setEnabled(true);
        u.setApprovedAt(LocalDateTime.now());
        User saved = userRepo.save(u);
        saveEnterpriseQualificationDecision(saved, "APPROVED");
        userChainArchiveService.anchorUserProfile(saved);
        userChainArchiveService.anchorUserEvent(saved, "USER_ENTERPRISE_FINAL_APPROVE");
        return Map.of("id", u.getId(), "status", "APPROVED", "statusLabel", "\u5df2\u5165\u9a7b");
    }

    @PutMapping("/enterprise-list/{id}/reject")
    public Map<String,Object> rejectEnterprise(Authentication authentication, @PathVariable Long id) {
        requirePlatformAdmin(authentication);
        User u = userRepo.findById(id).orElseThrow(() -> new RuntimeException("not found"));
        u.setApproved(false);
        u.setStatus("REJECTED");
        User saved = userRepo.save(u);
        saveEnterpriseQualificationDecision(saved, "REJECTED");
        userChainArchiveService.anchorUserProfile(saved);
        userChainArchiveService.anchorUserEvent(saved, "USER_ENTERPRISE_REJECT");
        return Map.of("id", u.getId(), "status", "REJECTED", "statusLabel", "\u5df2\u9a73\u56de");
    }

    @GetMapping("/enterprise-qualification/current")
    public Map<String,Object> getCurrentEnterpriseQualification(Authentication authentication) {
        User user = currentUser(authentication);
        if (user.getRole() == null || !user.getRole().startsWith("ENTERPRISE")) {
            throw new RuntimeException("Only enterprise users can view qualification status");
        }
        Map<String,Object> result = new HashMap<>();
        result.put("enterprise", mapEnterpriseListItem(user));
        approvalRepo.findFirstByUserIdAndTypeOrderByIdDesc(user.getId(), ENTERPRISE_QUALIFICATION)
                .ifPresent(approval -> result.put("approval", mapApproval(approval)));
        return result;
    }

    @PostMapping("/enterprise-qualification")
    public Map<String,Object> submitEnterpriseQualification(Authentication authentication, @RequestBody Map<String,Object> data) {
        User user = currentUser(authentication);
        if (user.getRole() == null || !user.getRole().startsWith("ENTERPRISE")) {
            throw new RuntimeException("Only enterprise users can submit qualification");
        }
        approvalRepo.findFirstByUserIdAndTypeOrderByIdDesc(user.getId(), ENTERPRISE_QUALIFICATION)
                .filter(approval -> "APPROVED".equals(approval.getStatus()))
                .ifPresent(approval -> { throw new RuntimeException("Enterprise qualification is already approved"); });
        approvalRepo.findFirstByUserIdAndTypeAndStatusOrderByIdDesc(user.getId(), ENTERPRISE_QUALIFICATION, "PENDING")
                .ifPresent(pending -> { throw new RuntimeException("浼佷笟璧勮川姝ｅ湪瀹℃牳涓紝璇峰嬁閲嶅鎻愪氦"); });
        String name = asString(data.get("name"));
        String code = asString(data.get("code"));
        String contact = asString(data.get("contact"));
        String phone = asString(data.get("phone"));
        if (name == null) throw new RuntimeException("Please enter enterprise name");
        if (code == null || code.length() != 18) throw new RuntimeException("Unified social credit code must be 18 characters");
        if (contact == null) throw new RuntimeException("璇疯緭鍏ヨ仈绯讳汉");
        if (phone == null) throw new RuntimeException("Please enter contact phone");
        if (!phone.matches("\\d{11}")) throw new RuntimeException("Contact phone must be 11 digits");
        if (userRepo.existsByPhoneAndIdNot(phone, user.getId())) throw new RuntimeException("Phone number already exists");
        userRepo.findByRoleAndOrganizationCode("ENTERPRISE_HR", code).stream()
                .filter(other -> !Objects.equals(other.getId(), user.getId()))
                .findFirst()
                .ifPresent(other -> { throw new RuntimeException("璇ヤ紒涓氫俊鐢ㄤ唬鐮佸凡缁戝畾鍏朵粬璐﹀彿"); });

        user.setOrganizationName(name);
        user.setOrganizationCode(code);
        user.setRealName(contact);
        user.setPhone(phone);
        user.setStatus("PENDING");
        user.setEnabled(true);
        User savedUser = userRepo.save(user);
        userChainArchiveService.anchorUserProfile(savedUser);
        userChainArchiveService.anchorUserEvent(savedUser, "USER_ENTERPRISE_QUALIFICATION_SUBMIT");

        Approval approval = approvalRepo
                .findFirstByUserIdAndTypeAndStatusOrderByIdDesc(savedUser.getId(), ENTERPRISE_QUALIFICATION, "PENDING")
                .orElseGet(Approval::new);
        approval.setUserId(savedUser.getId());
        approval.setType(ENTERPRISE_QUALIFICATION);
        approval.setName(name);
        approval.setCode(code);
        approval.setContact(contact);
        approval.setPhone(phone);
        approval.setStatus("PENDING");
        approval.setDate(LocalDate.now().toString());
        Approval savedApproval = approvalRepo.save(approval);
        anchorOnChain("浼佷笟璧勮川鎻愪氦", "浼佷笟:" + name, "enterprise-qualification:" + savedApproval.getId() + "|" + savedUser.getId(),
                null, savedUser.getId());

        Map<String,Object> result = new HashMap<>();
        result.put("enterprise", mapEnterpriseListItem(savedUser));
        result.put("approval", mapApproval(savedApproval));
        return result;
    }

    @PutMapping("/hired/sign")
    public Map<String,Object> signAgreement(Authentication authentication, @RequestBody Map<String,Object> data) {
        Long internshipId = asLong(data.get("internshipId"));
        Long studentId = asLong(data.get("studentId"));
        Long companyId = firstLong(data.get("companyId"), data.get("enterpriseId"));
        InternshipInfo internship = requireInternshipForAssessment(studentId, companyId, internshipId);
        requireEnterpriseActor(authentication, internship.getEnterpriseId());
        internship.setAgreementSigned(true);
        internship.setAgreementSignedAt(LocalDate.now().toString());
        InternshipInfo saved = internshipRepo.save(internship);
        Archive archive = anchorOnChain("鍗忚绛剧讲", "瀛︾敓:" + saved.getStudentName(),
                "agreement-sign:" + saved.getId() + "|" + saved.getStudentId() + "|" + saved.getEnterpriseId(),
                saved.getStudentId(), saved.getEnterpriseId(), saved.getId(), "internship:" + saved.getId());
        Map<String,Object> result = mapInternship(saved);
        result.put("signed", true);
        putChainResult(result, archive);
        return result;
    }

    // ===== Messages =====
    @GetMapping("/messages")
    public List<Map<String,Object>> getMessages(Authentication authentication) {
        return messageRepo.findAll().stream()
                .filter(message -> messageVisibleTo(authentication, message))
                .map(this::mapMessage)
                .collect(Collectors.toList());
    }

    @PutMapping("/messages/{id}/read")
    public Map<String,Object> markMessageRead(Authentication authentication, @PathVariable Long id) {
        PlatformMessage m = messageRepo.findById(id).orElseThrow(() -> new RuntimeException("not found"));
        if (!messageVisibleTo(authentication, m)) throw new RuntimeException("No permission to read this message");
        m.setReadFlag(true);
        PlatformMessage saved = messageRepo.save(m);
        return mapMessage(saved);
    }

    // ===== Identity Verification =====
    @GetMapping("/identity-approval/current")
    public Map<String,Object> getCurrentIdentityApproval(Authentication authentication) {
        User user = currentUser(authentication);
        Map<String,Object> result = new HashMap<>();
        result.put("identityStatus", firstText(user.getIdentityStatus(), "UNSUBMITTED"));
        result.put("user", mapUserProfile(user));
        approvalRepo.findFirstByUserIdAndTypeOrderByIdDesc(user.getId(), IDENTITY_APPROVAL)
                .ifPresent(approval -> result.put("approval", mapApproval(approval)));
        return result;
    }

    @PostMapping("/identity-approvals")
    public Map<String,Object> submitIdentityApproval(Authentication authentication, @RequestBody Map<String,Object> data) {
        User user = currentUser(authentication);
        if (!"STUDENT".equals(user.getRole())) throw new RuntimeException("Only students can submit identity verification");
        if ("APPROVED".equals(user.getIdentityStatus())) {
            throw new RuntimeException("Identity verification is already approved");
        }
        approvalRepo.findFirstByUserIdAndTypeAndStatusOrderByIdDesc(user.getId(), IDENTITY_APPROVAL, "PENDING")
                .ifPresent(pending -> { throw new RuntimeException("瀹炲悕璁よ瘉姝ｅ湪瀹℃牳涓紝璇峰嬁閲嶅鎻愪氦"); });
        String realName = asString(data.get("realName"));
        String idCard = asString(data.get("idCard"));
        String major = asString(data.get("major"));
        if (user.getSchoolId() == null) throw new RuntimeException("Account is not bound to a school");
        String school = resolveSchoolName(user.getSchoolId(), user.getOrganizationName());
        if (realName == null) throw new RuntimeException("Please enter real name");
        if (idCard == null || !idCard.matches("\\d{17}[\\dXx]")) throw new RuntimeException("Please enter a valid ID card number");
        if (major == null) throw new RuntimeException("Please enter major");
        if (user.getIdCard() != null && !user.getIdCard().isBlank() && !user.getIdCard().equalsIgnoreCase(idCard)) {
            throw new RuntimeException("ID card does not match the bound student roster");
        }
        boolean duplicated = userRepo.findByRoleAndIdCard("STUDENT", idCard).stream()
                .anyMatch(other -> !Objects.equals(other.getId(), user.getId()));
        if (duplicated) throw new RuntimeException("璇ュ鐢熻韩浠借瘉鍙峰凡瀛樺湪");

        Approval approval = approvalRepo
                .findFirstByUserIdAndTypeAndStatusOrderByIdDesc(user.getId(), IDENTITY_APPROVAL, "PENDING")
                .orElseGet(Approval::new);
        approval.setUserId(user.getId());
        approval.setType(IDENTITY_APPROVAL);
        approval.setName(realName);
        approval.setCode(idCard);
        approval.setSchool(school);
        approval.setMajor(major);
        approval.setContact(school);
        approval.setPhone(user.getPhone());
        approval.setStatus("PENDING");
        approval.setDate(LocalDate.now().toString());
        Approval saved = approvalRepo.save(approval);

        user.setIdentityStatus("PENDING");
        user.setOrganizationName(school);
        User savedUser = userRepo.save(user);
        userChainArchiveService.anchorUserProfile(savedUser);
        userChainArchiveService.anchorUserEvent(savedUser, "USER_IDENTITY_SUBMIT");
        anchorOnChain("瀹炲悕璁よ瘉鎻愪氦", "瀛︾敓:" + realName, "identity-submit:" + saved.getId() + "|" + saved.getUserId(),
                saved.getUserId(), null);
        return mapApproval(saved);
    }

    // ===== Approvals =====
    @GetMapping("/approvals")
    public List<Map<String,Object>> getApprovals(Authentication authentication) {
        User reviewer = currentUser(authentication);
        if (!"PLATFORM_ADMIN".equals(reviewer.getRole())) {
            throw new RuntimeException("Only platform admin can view platform approvals");
        }
        return approvalRepo.findAll().stream()
                .filter(approval -> ENTERPRISE_QUALIFICATION.equals(approval.getType()))
                .map(this::mapApproval)
                .collect(Collectors.toList());
    }

    @GetMapping("/approvals/school")
    public List<Map<String,Object>> getSchoolApprovals(Authentication authentication) {
        User school = currentUser(authentication);
        if (!"SCHOOL_ADMIN".equals(school.getRole())) {
            throw new RuntimeException("Only school admin can view school approvals");
        }
        return approvalRepo.findAll().stream()
                .filter(approval -> approvalBelongsToSchool(approval, school))
                .map(this::mapApproval)
                .collect(Collectors.toList());
    }

    @PutMapping("/approvals/{id}")
    public Map<String,Object> updateApproval(Authentication authentication, @PathVariable Long id, @RequestBody Map<String,Object> data) {
        User reviewer = currentUser(authentication);
        String newStatus = (String)data.get("status");
        Approval approval = approvalRepo.findById(id).orElseThrow(() -> new RuntimeException("not found"));
        if (!"APPROVED".equals(newStatus) && !"REJECTED".equals(newStatus)) {
            throw new RuntimeException("Unsupported approval status");
        }
        enforceApprovalReviewer(approval, reviewer);
        approval.setStatus(newStatus);
        Approval saved = approvalRepo.save(approval);
        if (IDENTITY_APPROVAL.equals(saved.getType())) {
            applyIdentityApproval(saved);
        } else if (ENTERPRISE_QUALIFICATION.equals(saved.getType())) {
            applyEnterpriseQualificationApproval(saved);
        }
        anchorOnChain("\u5ba1\u6279\u5904\u7406", "\u5ba1\u6279#" + id + "-" + ("APPROVED".equals(newStatus) ? "\u901a\u8fc7" : "\u9a73\u56de"),
                "approval:" + id + "|" + saved.getType() + "|" + newStatus, saved.getUserId(), null);
        return mapApproval(saved);
    }

    private Map<String,Object> mapJob(Job job) {
        Map<String,Object> m = new HashMap<>();
        String companyName = firstText(enterpriseDisplayName(job.getCompanyId()), job.getCompany());
        long applicationCount = appRepo.findByJobId(job.getId()).stream()
                .filter(app -> !"rejected".equals(app.getStatus()))
                .count();
        m.put("id", job.getId());
        m.put("title", jobDisplayTitle(job));
        m.put("companyId", job.getCompanyId());
        m.put("company", companyName);
        m.put("location", job.getLocation());
        m.put("salary", job.getSalary());
        m.put("type", job.getType());
        m.put("description", job.getDescription());
        m.put("status", job.getStatus());
        m.put("count", (int) applicationCount);
        return m;
    }

    private Map<String,Object> mapApplication(Application app) {
        Map<String,Object> m = new HashMap<>();
        Job job = resolveJobForApplication(app);
        User student = app.getStudentId() != null ? userRepo.findById(app.getStudentId()).orElse(null) : null;
        Long companyId = job != null ? job.getCompanyId() : app.getCompanyId();
        String jobTitle = jobDisplayTitle(job, app.getJobTitle());
        m.put("id", app.getId());
        m.put("studentId", app.getStudentId());
        m.put("name", firstDisplayText(student != null ? student.getRealName() : null, app.getName(), student != null ? student.getUsername() : null));
        m.put("jobId", app.getJobId());
        m.put("jobTitle", jobTitle);
        m.put("school", studentSchoolName(student, app.getSchool()));
        m.put("major", studentMajor(student, app.getMajor()));
        m.put("applyDate", app.getApplyDate());
        m.put("status", app.getStatus());
        m.put("companyId", companyId);
        m.put("company", firstText(enterpriseDisplayName(companyId), job != null ? job.getCompany() : null));
        return m;
    }

    private Map<String,Object> mapUserProfile(User user) {
        Map<String,Object> m = new HashMap<>();
        m.put("id", user.getId());
        m.put("userId", user.getId());
        m.put("username", user.getUsername());
        m.put("realName", user.getRealName());
        m.put("role", user.getRole());
        m.put("schoolId", user.getSchoolId());
        m.put("organizationName", userOrganizationName(user));
        m.put("major", userMajor(user));
        m.put("email", user.getEmail());
        m.put("phone", user.getPhone());
        m.put("walletAddress", user.getWalletAddress());
        m.put("status", user.getStatus());
        m.put("identityStatus", user.getIdentityStatus());
        return m;
    }

    private String userOrganizationName(User user) {
        if (user == null) return null;
        if ("STUDENT".equals(user.getRole())) {
            return studentSchoolName(user);
        }
        if ("SCHOOL_ADMIN".equals(user.getRole())) {
            return firstDisplayText(user.getOrganizationName(), user.getRealName(), user.getUsername());
        }
        return firstDisplayText(user.getOrganizationName(), user.getRealName(), user.getUsername());
    }

    private String userMajor(User user) {
        if (user == null) return null;
        if ("STUDENT".equals(user.getRole())) return studentMajor(user);
        return firstDisplayText(user.getMajor());
    }

    private Map<String,Object> mapEnterpriseListItem(User u) {
        Map<String,Object> m = new HashMap<>();
        String status = enterpriseQualificationStatus(u);
        m.put("id", u.getId());
        m.put("name", firstText(u.getOrganizationName(), u.getRealName(), u.getUsername()));
        m.put("code", firstText(u.getOrganizationCode(), "-"));
        m.put("industry", "\u4fe1\u606f\u6280\u672f");
        m.put("scale", "涓瀷");
        m.put("address", "-");
        m.put("contact", firstText(u.getRealName(), "-"));
        m.put("phone", firstText(u.getPhone(), "-"));
        m.put("email", firstText(u.getEmail(), "-"));
        m.put("status", status);
        m.put("statusLabel", switch (status) {
            case "APPROVED" -> "\u5df2\u5165\u9a7b";
            case "REJECTED" -> "\u5df2\u9a73\u56de";
            case "ROSTER" -> "\u540d\u5355\u5f85\u6ce8\u518c";
            case "UNSUBMITTED" -> "\u5f85\u63d0\u4ea4\u8d44\u8d28";
            default -> "\u5e73\u53f0\u5ba1\u6838\u4e2d";
        });
        m.put("statusColor", switch (status) {
            case "APPROVED" -> "green";
            case "REJECTED" -> "red";
            case "ROSTER" -> "blue";
            case "UNSUBMITTED" -> "gray";
            default -> "orange";
        });
        return m;
    }

    private String enterpriseQualificationStatus(User u) {
        if ("ROSTER".equals(u.getStatus())) return "ROSTER";
        Optional<Approval> approval = approvalRepo.findFirstByUserIdAndTypeOrderByIdDesc(u.getId(), ENTERPRISE_QUALIFICATION);
        if (approval.isPresent()) return firstText(approval.get().getStatus(), "PENDING");
        return "UNSUBMITTED";
    }

    private String uniqueRosterUsername(String prefix, String code) {
        String sanitized = code == null ? String.valueOf(System.currentTimeMillis()) : code.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
        if (sanitized.length() > 24) sanitized = sanitized.substring(0, 24);
        String base = "roster_" + prefix + "_" + sanitized;
        String username = base;
        int counter = 1;
        while (userRepo.existsByUsername(username)) {
            username = base + "_" + counter++;
        }
        return username;
    }

    private String enterpriseDisplayName(Long companyId) {
        if (companyId == null) return null;
        return userRepo.findById(companyId)
                .map(u -> firstText(u.getOrganizationName(), u.getRealName(), u.getUsername()))
                .orElse(null);
    }

    private void putChainResult(Map<String,Object> result, Archive archive) {
        if (archive == null) return;
        result.put("chainStatus", archive.getChainStatus());
        result.put("txHash", archive.getTxHash());
        result.put("chainError", archive.getChainError());
    }

    private Map<String,Object> mapArchive(Archive a) {
        Map<String,Object> m = new HashMap<>();
        m.put("id", a.getId());
        m.put("type", a.getType());
        m.put("name", a.getName());
        m.put("hash", a.getHash());
        m.put("time", a.getTime());
        m.put("block", a.getBlock());
        m.put("studentId", a.getStudentId());
        m.put("companyId", a.getCompanyId());
        m.put("internshipId", a.getInternshipId());
        m.put("sourceId", a.getSourceId());
        m.put("chainStatus", a.getChainStatus());
        m.put("txHash", a.getTxHash());
        m.put("chainError", a.getChainError());
        return m;
    }

    private Archive backfillArchiveLinks(Archive archive) {
        boolean dirty = false;
        String type = archive.getType();
        String sourceId = archive.getSourceId();

        if (("鑰冩牳鎻愪氦".equals(type) || "閼板啯鐗抽幓鎰唉".equals(type)) && isPlainNumber(sourceId)) {
            Long assessmentId = Long.valueOf(sourceId);
            archive.setSourceId("assessment:" + assessmentId);
            dirty = true;
            Assessment assessment = assessRepo.findById(assessmentId).orElse(null);
            if (assessment != null && archive.getInternshipId() == null) {
                archive.setInternshipId(assessment.getInternshipId());
                dirty = true;
            }
        }

        if (("绾犵悍鍙戣捣".equals(type) || "绾犵悍澶勭悊".equals(type)
                || "缁剧姷鎮嶉崣鎴ｆ崳".equals(type) || "缁剧姷鎮嶆径鍕倞".equals(type)) && isPlainNumber(sourceId)) {
            Long disputeId = Long.valueOf(sourceId);
            archive.setSourceId("dispute:" + disputeId);
            dirty = true;
            Dispute dispute = disputeRepo.findById(disputeId).orElse(null);
            if (dispute != null && archive.getInternshipId() == null && dispute.getAssessmentId() != null) {
                Long internshipId = assessRepo.findById(dispute.getAssessmentId())
                        .map(Assessment::getInternshipId)
                        .orElse(null);
                archive.setInternshipId(internshipId);
                dirty = true;
            }
        }

        if (("瀹炰範鐧昏".equals(type) || "鐎圭偘绡勯惂鏄忣唶".equals(type))
                && (archive.getInternshipId() == null || archive.getSourceId() == null)) {
            InternshipInfo internship = findLikelyInternship(archive.getStudentId(), archive.getCompanyId());
            if (internship != null) {
                if (archive.getInternshipId() == null) archive.setInternshipId(internship.getId());
                if (archive.getSourceId() == null) archive.setSourceId("internship:" + internship.getId());
                dirty = true;
            }
        }

        if (("APPLICATION_SUBMIT".equals(type) || "APPLICATION_UPDATE".equals(type)) && archive.getSourceId() == null) {
            Application application = findLikelyApplication(archive.getStudentId(), archive.getCompanyId());
            if (application != null) {
                archive.setSourceId("application:" + application.getId());
                dirty = true;
            }
            InternshipInfo internship = findLikelyInternship(archive.getStudentId(), archive.getCompanyId());
            if (internship != null && archive.getInternshipId() == null) {
                archive.setInternshipId(internship.getId());
                dirty = true;
            }
        }

        if (type != null && (type.startsWith("宀椾綅") || type.startsWith("瀹€妞剧秴")) && archive.getSourceId() == null) {
            Job job = findLikelyJob(archive.getCompanyId(), archive.getName());
            if (job != null) {
                archive.setSourceId("job:" + job.getId());
                dirty = true;
            }
        }

        return dirty ? archiveRepo.save(archive) : archive;
    }

    private boolean isPlainNumber(String value) {
        return value != null && value.matches("\\d+");
    }

    private InternshipInfo findLikelyInternship(Long studentId, Long companyId) {
        if (studentId == null || companyId == null) return null;
        return internshipRepo.findByStudentIdAndEnterpriseId(studentId, companyId).stream()
                .max(Comparator.comparing(InternshipInfo::getId))
                .orElse(null);
    }

    private Application findLikelyApplication(Long studentId, Long companyId) {
        if (studentId == null || companyId == null) return null;
        return appRepo.findByStudentIdAndCompanyId(studentId, companyId).stream()
                .max(Comparator.comparing(Application::getId))
                .orElse(null);
    }

    private Job findLikelyJob(Long companyId, String archiveName) {
        if (companyId == null || archiveName == null) return null;
        String title = archiveName.startsWith("宀椾綅:") ? archiveName.substring("宀椾綅:".length()) :
                archiveName.startsWith("瀹€妞剧秴:") ? archiveName.substring("瀹€妞剧秴:".length()) : archiveName;
        return jobRepo.findByCompanyId(companyId).stream()
                .filter(job -> Objects.equals(job.getTitle(), title))
                .max(Comparator.comparing(Job::getId))
                .orElse(null);
    }

    private User getApprovedEnterprise(Long id) {
        if (id == null) throw new RuntimeException("Company is required");
        User enterprise = userRepo.findById(id).orElseThrow(() -> new RuntimeException("Company does not exist"));
        String role = enterprise.getRole();
        if (role == null || !role.startsWith("ENTERPRISE")) throw new RuntimeException("User is not an enterprise");
        if (Boolean.FALSE.equals(enterprise.getEnabled())) throw new RuntimeException("Enterprise account is disabled");
        if (!isEnterpriseQualificationApproved(enterprise.getId())) {
            throw new RuntimeException("Enterprise qualification is not approved");
        }
        return enterprise;
    }

    private boolean isEnterpriseQualificationApproved(Long enterpriseId) {
        return enterpriseId != null && approvalRepo.findFirstByUserIdAndTypeOrderByIdDesc(enterpriseId, ENTERPRISE_QUALIFICATION)
                .map(approval -> "APPROVED".equals(approval.getStatus()))
                .orElse(false);
    }

    private boolean isApprovedEnterprise(Long id) {
        try {
            getApprovedEnterprise(id);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private Long resolveViewerSchoolId(Authentication authentication, Long requestedSchoolId) {
        if (authentication == null || authentication.getName() == null) return requestedSchoolId;
        return userRepo.findByUsername(authentication.getName())
                .filter(user -> "SCHOOL_ADMIN".equals(user.getRole()) || "STUDENT".equals(user.getRole()))
                .map(this::viewerSchoolId)
                .orElse(requestedSchoolId);
    }

    private Long resolveWritableSchoolId(Authentication authentication, Long requestedSchoolId) {
        User viewer = currentUser(authentication);
        if ("SCHOOL_ADMIN".equals(viewer.getRole())) {
            Long schoolId = viewerSchoolId(viewer);
            if (schoolId == null) throw new RuntimeException("School account is not bound to a school");
            if (requestedSchoolId != null && !Objects.equals(requestedSchoolId, schoolId)) {
                throw new RuntimeException("No permission to manage students from another school");
            }
            return schoolId;
        }
        if ("PLATFORM_ADMIN".equals(viewer.getRole())) return requestedSchoolId;
        throw new RuntimeException("Only school or platform admin can manage student roster");
    }

    private void enforceSchoolStudentWritable(Authentication authentication, User student) {
        User viewer = currentUser(authentication);
        if ("PLATFORM_ADMIN".equals(viewer.getRole())) return;
        if ("SCHOOL_ADMIN".equals(viewer.getRole()) && studentBelongsToSchool(student.getId(), viewerSchoolId(viewer))) return;
        throw new RuntimeException("No permission to manage this student");
    }

    private void requireEnterpriseActor(Authentication authentication, Long enterpriseId) {
        User viewer = currentUser(authentication);
        if ("PLATFORM_ADMIN".equals(viewer.getRole())) return;
        if (viewer.getRole() != null && viewer.getRole().startsWith("ENTERPRISE") && Objects.equals(viewer.getId(), enterpriseId)) return;
        throw new RuntimeException("No permission to manage this enterprise data");
    }

    private void requireStudentActor(Authentication authentication, Long studentId) {
        User viewer = currentUser(authentication);
        if ("PLATFORM_ADMIN".equals(viewer.getRole())) return;
        if ("STUDENT".equals(viewer.getRole()) && Objects.equals(viewer.getId(), studentId)) return;
        throw new RuntimeException("No permission to manage this student data");
    }

    private void requireSchoolOrPlatformAdmin(Authentication authentication) {
        User viewer = currentUser(authentication);
        if ("SCHOOL_ADMIN".equals(viewer.getRole()) || "PLATFORM_ADMIN".equals(viewer.getRole())) return;
        throw new RuntimeException("Only school or platform admin can perform this operation");
    }

    private void enforceArchiveWritable(Authentication authentication, Archive archive) {
        User viewer = currentUser(authentication);
        if ("PLATFORM_ADMIN".equals(viewer.getRole())) return;
        if ("STUDENT".equals(viewer.getRole())) {
            if (archive.getStudentId() == null) archive.setStudentId(viewer.getId());
            if (Objects.equals(archive.getStudentId(), viewer.getId())) return;
            throw new RuntimeException("No permission to archive another student's data");
        }
        if (viewer.getRole() != null && viewer.getRole().startsWith("ENTERPRISE")) {
            if (archive.getCompanyId() == null) archive.setCompanyId(viewer.getId());
            if (Objects.equals(archive.getCompanyId(), viewer.getId())) return;
            throw new RuntimeException("No permission to archive another enterprise's data");
        }
        if ("SCHOOL_ADMIN".equals(viewer.getRole())) {
            Long schoolId = viewerSchoolId(viewer);
            if (archive.getStudentId() != null && studentBelongsToSchool(archive.getStudentId(), schoolId)) return;
            if (archive.getInternshipId() != null && internshipRepo.findById(archive.getInternshipId())
                    .map(internship -> internshipBelongsToSchool(internship, schoolId))
                    .orElse(false)) return;
            throw new RuntimeException("No permission to archive data outside this school");
        }
        throw new RuntimeException("No permission to archive data");
    }

    private Long viewerSchoolId(User user) {
        if (user == null) return null;
        if ("SCHOOL_ADMIN".equals(user.getRole())) return user.getSchoolId() != null ? user.getSchoolId() : user.getId();
        if ("STUDENT".equals(user.getRole())) return user.getSchoolId();
        return null;
    }

    private User currentUserOrNull(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) return null;
        return userRepo.findByUsername(authentication.getName()).orElse(null);
    }

    private boolean applicationBelongsToSchool(Application app, Long schoolId) {
        if (app == null || schoolId == null) return false;
        if (app.getStudentId() != null) {
            Optional<User> student = userRepo.findById(app.getStudentId());
            if (student.isPresent() && Objects.equals(student.get().getSchoolId(), schoolId)) return true;
        }
        String schoolName = resolveSchoolName(schoolId, null);
        return sameText(app.getSchool(), schoolName);
    }

    private boolean studentBelongsToSchool(Long studentId, Long schoolId) {
        if (studentId == null || schoolId == null) return false;
        return userRepo.findById(studentId)
                .map(student -> Objects.equals(student.getSchoolId(), schoolId))
                .orElse(false);
    }

    private boolean internshipBelongsToSchool(InternshipInfo internship, Long schoolId) {
        if (internship == null || schoolId == null) return false;
        if (internship.getStudentId() != null) return studentBelongsToSchool(internship.getStudentId(), schoolId);
        return Objects.equals(internship.getSchoolId(), schoolId);
    }

    private boolean reportBelongsToSchool(Report report, Long schoolId) {
        if (report == null || schoolId == null) return false;
        if (studentBelongsToSchool(report.getStudentId(), schoolId)) return true;
        if (report.getInternshipId() != null) {
            return internshipRepo.findById(report.getInternshipId())
                    .map(internship -> internshipBelongsToSchool(internship, schoolId))
                    .orElse(false);
        }
        return false;
    }

    private boolean assessmentBelongsToSchool(Assessment assessment, Long schoolId) {
        if (assessment == null || schoolId == null) return false;
        if (studentBelongsToSchool(assessment.getStudentId(), schoolId)) return true;
        if (assessment.getInternshipId() != null) {
            return internshipRepo.findById(assessment.getInternshipId())
                    .map(internship -> internshipBelongsToSchool(internship, schoolId))
                    .orElse(false);
        }
        return false;
    }

    private boolean disputeBelongsToSchool(Dispute dispute, Long schoolId) {
        if (dispute == null || schoolId == null) return false;
        if (studentBelongsToSchool(dispute.getStudentId(), schoolId)) return true;
        if (dispute.getAssessmentId() != null) {
            return assessRepo.findById(dispute.getAssessmentId())
                    .map(assessment -> assessmentBelongsToSchool(assessment, schoolId))
                    .orElse(false);
        }
        return false;
    }

    private boolean archiveBelongsToSchool(Archive archive, Long schoolId) {
        if (archive == null || schoolId == null) return false;
        if (studentBelongsToSchool(archive.getStudentId(), schoolId)) return true;
        if (archive.getInternshipId() != null) {
            return internshipRepo.findById(archive.getInternshipId())
                    .map(internship -> internshipBelongsToSchool(internship, schoolId))
                    .orElse(false);
        }
        return false;
    }

    private boolean applicationVisibleTo(Authentication authentication, Application app) {
        User viewer = currentUserOrNull(authentication);
        if (viewer == null) return false;
        if ("PLATFORM_ADMIN".equals(viewer.getRole())) return true;
        if ("SCHOOL_ADMIN".equals(viewer.getRole())) return applicationBelongsToSchool(app, viewerSchoolId(viewer));
        if ("STUDENT".equals(viewer.getRole())) return Objects.equals(app.getStudentId(), viewer.getId());
        if (viewer.getRole() != null && viewer.getRole().startsWith("ENTERPRISE")) return Objects.equals(app.getCompanyId(), viewer.getId());
        return false;
    }

    private boolean internshipVisibleTo(Authentication authentication, InternshipInfo internship) {
        User viewer = currentUserOrNull(authentication);
        if (viewer == null) return false;
        if ("PLATFORM_ADMIN".equals(viewer.getRole())) return true;
        if ("SCHOOL_ADMIN".equals(viewer.getRole())) return internshipBelongsToSchool(internship, viewerSchoolId(viewer));
        if ("STUDENT".equals(viewer.getRole())) return Objects.equals(internship.getStudentId(), viewer.getId());
        if (viewer.getRole() != null && viewer.getRole().startsWith("ENTERPRISE")) return Objects.equals(internship.getEnterpriseId(), viewer.getId());
        return false;
    }

    private boolean assessmentVisibleTo(Authentication authentication, Assessment assessment) {
        User viewer = currentUserOrNull(authentication);
        if (viewer == null) return false;
        if ("PLATFORM_ADMIN".equals(viewer.getRole())) return true;
        if ("SCHOOL_ADMIN".equals(viewer.getRole())) return assessmentBelongsToSchool(assessment, viewerSchoolId(viewer));
        if ("STUDENT".equals(viewer.getRole())) return Objects.equals(assessment.getStudentId(), viewer.getId());
        if (viewer.getRole() != null && viewer.getRole().startsWith("ENTERPRISE")) return Objects.equals(assessment.getCompanyId(), viewer.getId());
        return false;
    }

    private boolean disputeVisibleTo(Authentication authentication, Dispute dispute) {
        User viewer = currentUserOrNull(authentication);
        if (viewer == null) return false;
        if ("PLATFORM_ADMIN".equals(viewer.getRole())) return true;
        if ("SCHOOL_ADMIN".equals(viewer.getRole())) return disputeBelongsToSchool(dispute, viewerSchoolId(viewer));
        if ("STUDENT".equals(viewer.getRole())) return Objects.equals(dispute.getStudentId(), viewer.getId());
        if (viewer.getRole() != null && viewer.getRole().startsWith("ENTERPRISE")) return Objects.equals(dispute.getCompanyId(), viewer.getId());
        return false;
    }

    private boolean archiveVisibleTo(Authentication authentication, Archive archive) {
        User viewer = currentUserOrNull(authentication);
        if (viewer == null) return false;
        if ("PLATFORM_ADMIN".equals(viewer.getRole())) return true;
        if ("SCHOOL_ADMIN".equals(viewer.getRole())) return archiveBelongsToSchool(archive, viewerSchoolId(viewer));
        if ("STUDENT".equals(viewer.getRole())) return Objects.equals(archive.getStudentId(), viewer.getId());
        if (viewer.getRole() != null && viewer.getRole().startsWith("ENTERPRISE")) return Objects.equals(archive.getCompanyId(), viewer.getId());
        return false;
    }

    private boolean reportVisibleTo(Authentication authentication, Report report) {
        User viewer = currentUserOrNull(authentication);
        if (viewer == null) return false;
        if ("PLATFORM_ADMIN".equals(viewer.getRole())) return true;
        if ("SCHOOL_ADMIN".equals(viewer.getRole())) return reportBelongsToSchool(report, viewerSchoolId(viewer));
        if ("STUDENT".equals(viewer.getRole())) return Objects.equals(report.getStudentId(), viewer.getId());
        if (viewer.getRole() != null && viewer.getRole().startsWith("ENTERPRISE")) return Objects.equals(report.getEnterpriseId(), viewer.getId());
        return false;
    }

    private boolean noticeVisibleTo(Authentication authentication, Notice notice) {
        User viewer = currentUserOrNull(authentication);
        if (viewer == null) return false;
        if ("PLATFORM_ADMIN".equals(viewer.getRole())) return true;
        Long noticeSchoolId = notice.getSchoolId();
        if (noticeSchoolId == null) return true;
        Long viewerSchoolId = viewerSchoolId(viewer);
        return viewerSchoolId != null && Objects.equals(noticeSchoolId, viewerSchoolId);
    }

    private boolean messageVisibleTo(Authentication authentication, PlatformMessage message) {
        User viewer = currentUserOrNull(authentication);
        if (viewer == null) return false;
        if ("PLATFORM_ADMIN".equals(viewer.getRole())) return true;
        Long messageSchoolId = message.getSchoolId();
        if (messageSchoolId == null) return true;
        Long viewerSchoolId = viewerSchoolId(viewer);
        return viewerSchoolId != null && Objects.equals(messageSchoolId, viewerSchoolId);
    }

    private User getApprovedStudent(Long id) {
        if (id == null) throw new RuntimeException("Student is required");
        User student = userRepo.findById(id).orElseThrow(() -> new RuntimeException("Student does not exist"));
        if (!"STUDENT".equals(student.getRole())) throw new RuntimeException("User is not a student");
        if (Boolean.FALSE.equals(student.getEnabled())) throw new RuntimeException("Student account is disabled");
        if (!Boolean.TRUE.equals(student.getApproved()) || "PENDING".equals(student.getStatus()) || "REJECTED".equals(student.getStatus())) {
            throw new RuntimeException("Student is not approved");
        }
        return student;
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Please login again");
        }
        return userRepo.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User does not exist"));
    }

    private User requirePlatformAdmin(Authentication authentication) {
        User user = currentUser(authentication);
        if (!"PLATFORM_ADMIN".equals(user.getRole())) {
            throw new RuntimeException("Only platform admin can process enterprise qualification");
        }
        return user;
    }

    private Application requireAcceptedApplication(Long studentId, Long enterpriseId) {
        return appRepo.findByStudentIdAndCompanyIdAndStatus(studentId, enterpriseId, "accepted").stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Enterprise has not confirmed this student as hired"));
    }

    private InternshipInfo requireActiveInternshipForReport(Long studentId, Long internshipId) {
        List<InternshipInfo> internships = internshipRepo.findByStudentId(studentId);
        return internships.stream()
                .filter(i -> internshipId == null || Objects.equals(i.getId(), internshipId))
                .filter(i -> "ACTIVE".equals(i.getStatus()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Please create an approved active internship record before submitting daily report"));
    }

    private InternshipInfo requireInternshipForAssessment(Long studentId, Long companyId, Long internshipId) {
        if (internshipId != null) {
            InternshipInfo internship = internshipRepo.findById(internshipId)
                    .orElseThrow(() -> new RuntimeException("Internship record does not exist"));
            if (studentId != null && !Objects.equals(studentId, internship.getStudentId())) {
                throw new RuntimeException("Student does not match internship record");
            }
            if (companyId != null && !Objects.equals(companyId, internship.getEnterpriseId())) {
                throw new RuntimeException("Company does not match internship record");
            }
            return internship;
        }
        if (studentId == null || companyId == null) {
            throw new RuntimeException("Student, company and internship relation are required");
        }
        List<InternshipInfo> list = internshipRepo.findByStudentIdAndEnterpriseId(studentId, companyId);
        return list.stream()
                .filter(i -> "ACTIVE".equals(i.getStatus()))
                .findFirst()
                .or(() -> list.stream().findFirst())
                .orElseThrow(() -> new RuntimeException("No internship relation exists between this student and company"));
    }

    private void applyIdentityApproval(Approval approval) {
        if (approval.getUserId() == null) throw new RuntimeException("Identity approval is missing user id");
        User user = userRepo.findById(approval.getUserId()).orElseThrow(() -> new RuntimeException("User does not exist"));
        if ("APPROVED".equals(approval.getStatus())) {
            user.setRealName(approval.getName());
            user.setIdCard(approval.getCode());
            user.setOrganizationName(approval.getSchool());
            user.setMajor(approval.getMajor());
            user.setIdentityStatus("APPROVED");
            user.setApproved(true);
            user.setStatus("APPROVED");
            user.setEnabled(true);
            user.setApprovedAt(LocalDateTime.now());
        } else if ("REJECTED".equals(approval.getStatus())) {
            user.setIdentityStatus("REJECTED");
            user.setApproved(false);
            user.setStatus("REJECTED");
        }
        User saved = userRepo.save(user);
        userChainArchiveService.anchorUserProfile(saved);
        userChainArchiveService.anchorUserEvent(saved, "USER_IDENTITY_" + approval.getStatus());
    }

    private void applyEnterpriseQualificationApproval(Approval approval) {
        if (approval.getUserId() == null) throw new RuntimeException("Enterprise approval is missing user id");
        User user = userRepo.findById(approval.getUserId()).orElseThrow(() -> new RuntimeException("User does not exist"));
        if ("APPROVED".equals(approval.getStatus())) {
            user.setApproved(true);
            user.setStatus("APPROVED");
            user.setEnabled(true);
            user.setApprovedAt(LocalDateTime.now());
        } else if ("REJECTED".equals(approval.getStatus())) {
            user.setApproved(false);
            user.setStatus("REJECTED");
        }
        User saved = userRepo.save(user);
        userChainArchiveService.anchorUserProfile(saved);
        userChainArchiveService.anchorUserEvent(saved, "USER_ENTERPRISE_QUALIFICATION_" + approval.getStatus());
    }

    private void saveEnterpriseQualificationDecision(User enterprise, String status) {
        Approval approval = approvalRepo
                .findFirstByUserIdAndTypeOrderByIdDesc(enterprise.getId(), ENTERPRISE_QUALIFICATION)
                .orElseGet(Approval::new);
        approval.setUserId(enterprise.getId());
        approval.setType(ENTERPRISE_QUALIFICATION);
        approval.setName(firstText(enterprise.getOrganizationName(), enterprise.getRealName(), enterprise.getUsername()));
        approval.setCode(enterprise.getOrganizationCode());
        approval.setContact(enterprise.getRealName());
        approval.setPhone(enterprise.getPhone());
        approval.setStatus(status);
        approval.setDate(LocalDate.now().toString());
        approvalRepo.save(approval);
    }

    private boolean approvalBelongsToSchool(Approval approval, User school) {
        if (approval == null || school == null) return false;
        if (!IDENTITY_APPROVAL.equals(approval.getType())) return false;
        if (approval.getUserId() == null) return false;
        return userRepo.findById(approval.getUserId())
                .map(user -> Objects.equals(user.getSchoolId(), school.getId())
                        || Objects.equals(user.getSchoolId(), school.getSchoolId()))
                .orElse(false);
    }

    private void enforceApprovalReviewer(Approval approval, User reviewer) {
        if (reviewer == null) throw new RuntimeException("Please login again");
        if (IDENTITY_APPROVAL.equals(approval.getType())) {
            if (!"SCHOOL_ADMIN".equals(reviewer.getRole()) || !approvalBelongsToSchool(approval, reviewer)) {
                throw new RuntimeException("Only the student school can approve identity verification");
            }
            return;
        }
        if (ENTERPRISE_QUALIFICATION.equals(approval.getType())) {
            if (!"PLATFORM_ADMIN".equals(reviewer.getRole())) {
                throw new RuntimeException("Only platform admin can approve enterprise qualification");
            }
            return;
        }
        throw new RuntimeException("Unsupported approval type");
    }

    private String resolveSchoolName(Long schoolId, String fallback) {
        if (schoolId != null) {
            Optional<User> school = userRepo.findById(schoolId);
            if (school.isPresent()) {
                return firstText(school.get().getOrganizationName(), school.get().getRealName(), school.get().getUsername(), fallback);
            }
        }
        return firstText(fallback, "\u672a\u7ed1\u5b9a\u5b66\u6821");
    }

    private Optional<User> resolveSchoolAdminByName(String schoolName) {
        if (schoolName == null || schoolName.isBlank()) return Optional.empty();
        return userRepo.findByRole("SCHOOL_ADMIN").stream()
                .filter(school -> sameText(school.getOrganizationName(), schoolName)
                        || sameText(school.getRealName(), schoolName)
                        || sameText(school.getUsername(), schoolName))
                .findFirst();
    }

    private boolean sameText(String left, String right) {
        return left != null && right != null && left.trim().equals(right.trim());
    }

    private void ensureStudentCanApply(Long studentId, Long currentApplicationId) {
        boolean hasAcceptedApplication = appRepo.findByStudentId(studentId).stream()
                .filter(app -> !Objects.equals(app.getId(), currentApplicationId))
                .anyMatch(app -> "accepted".equals(app.getStatus()));
        boolean hasActiveInternship = internshipRepo.findByStudentId(studentId).stream()
                .anyMatch(internship -> "ACTIVE".equals(internship.getStatus()));
        if (hasAcceptedApplication || hasActiveInternship) {
            throw new RuntimeException("Student already has an accepted application or active internship");
        }
    }

    private void refreshJobCount(Long jobId) {
        if (jobId == null) return;
        jobRepo.findById(jobId).ifPresent(job -> {
            long count = appRepo.findByJobId(jobId).stream()
                    .filter(app -> !"rejected".equals(app.getStatus()))
                    .count();
            job.setCount((int) count);
            jobRepo.save(job);
        });
    }

    private void createInternshipForAcceptedApplication(Application app, Map<String,Object> data) {
        if (app.getStudentId() == null) return;
        boolean hasActive = internshipRepo.findByStudentId(app.getStudentId()).stream()
                .anyMatch(internship -> "ACTIVE".equals(internship.getStatus()));
        if (hasActive) return;

        User student = userRepo.findById(app.getStudentId()).orElse(null);
        User enterprise = app.getCompanyId() != null ? userRepo.findById(app.getCompanyId()).orElse(null) : null;
        Job job = app.getJobId() != null ? jobRepo.findById(app.getJobId()).orElse(null) : null;
        LocalDate startDate = asDate(data.get("startDate"));
        if (startDate == null) startDate = LocalDate.now();

        InternshipInfo internship = new InternshipInfo();
        internship.setStudentId(app.getStudentId());
        internship.setStudentName(firstDisplayText(student != null ? student.getRealName() : null, app.getName(), student != null ? student.getUsername() : null));
        internship.setStudentIdCard(student != null ? student.getIdCard() : null);
        internship.setEnterpriseId(app.getCompanyId());
        internship.setEnterpriseName(firstText(
                enterprise != null ? enterprise.getOrganizationName() : null,
                enterprise != null ? enterprise.getRealName() : null,
                app.getCompanyId() != null ? "Company#" + app.getCompanyId() : null
        ));
        internship.setEnterpriseCode(enterprise != null ? enterprise.getOrganizationCode() : null);
        internship.setSchoolId(student != null ? student.getSchoolId() : null);
        internship.setSchoolName(studentSchoolName(student, app.getSchool()));
        internship.setPosition(jobDisplayTitle(job, app.getJobTitle(), data.get("position")));
        internship.setStartDate(startDate);
        internship.setStatus("ACTIVE");
        internship.setContentHash(BlockchainService.calculateHash(
                internship.getStudentId() + "|" + internship.getEnterpriseId() + "|" + internship.getPosition() + "|" + internship.getStartDate()
        ));

        InternshipInfo saved = internshipRepo.save(internship);
        Archive archive = anchorOnChain("瀹炰範鐧昏", "瀛︾敓:" + saved.getStudentName(),
                "internship:" + saved.getId() + ":" + saved.getContentHash(),
                saved.getStudentId(), saved.getEnterpriseId(), saved.getId(), "internship:" + saved.getId());
        if (archive != null && archive.getTxHash() != null) {
            saved.setBlockchainTxHash(archive.getTxHash());
            internshipRepo.save(saved);
        }
    }

    private String firstText(Object... values) {
        for (Object value : values) {
            String text = asString(value);
            if (text != null && !isBrokenText(text)) return text;
        }
        return null;
    }

    private String firstDisplayText(Object... values) {
        for (Object value : values) {
            String text = asString(value);
            if (text != null && !isBrokenText(text) && !isPlaceholderText(text)) return text;
        }
        return null;
    }

    private String firstJobDisplayText(Object... values) {
        for (Object value : values) {
            String text = asString(value);
            if (text != null && !isBrokenText(text) && !isPlaceholderText(text) && !isPlainNumber(text)) return text;
        }
        return null;
    }

    private String jobDisplayTitle(Job job, Object... fallbacks) {
        List<Object> values = new ArrayList<>();
        values.add(job != null ? job.getTitle() : null);
        values.addAll(Arrays.asList(fallbacks));
        String title = firstJobDisplayText(values.toArray());
        if (title != null) return title;
        return fallbackJobTitle(job);
    }

    private String fallbackJobTitle(Job job) {
        if (job == null || job.getCompanyId() == null) return null;
        List<Job> jobs = jobRepo.findByCompanyId(job.getCompanyId()).stream()
                .filter(candidate -> !Objects.equals(candidate.getId(), job.getId()))
                .sorted(Comparator.comparing(Job::getId, Comparator.nullsLast(Long::compareTo)).reversed())
                .collect(Collectors.toList());
        Optional<String> sameStatus = jobs.stream()
                .filter(candidate -> Objects.equals(candidate.getStatus(), job.getStatus()))
                .map(Job::getTitle)
                .map(this::firstJobDisplayText)
                .filter(Objects::nonNull)
                .findFirst();
        return sameStatus.orElseGet(() -> jobs.stream()
                .map(Job::getTitle)
                .map(this::firstJobDisplayText)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null));
    }

    private String studentSchoolName(User student, Object... fallbacks) {
        Approval identity = studentIdentityApproval(student);
        List<Object> values = new ArrayList<>();
        values.add(student != null ? student.getOrganizationName() : null);
        values.add(identity != null ? identity.getSchool() : null);
        values.add(student != null && student.getSchoolId() != null ? resolveSchoolName(student.getSchoolId(), null) : null);
        values.addAll(Arrays.asList(fallbacks));
        return firstDisplayText(values.toArray());
    }

    private String studentMajor(User student, Object... fallbacks) {
        Approval identity = studentIdentityApproval(student);
        List<Object> values = new ArrayList<>();
        values.add(student != null ? student.getMajor() : null);
        values.add(identity != null ? identity.getMajor() : null);
        values.addAll(Arrays.asList(fallbacks));
        return firstDisplayText(values.toArray());
    }

    private Approval studentIdentityApproval(User student) {
        if (student == null || student.getId() == null) return null;
        return approvalRepo.findFirstByUserIdAndTypeAndStatusOrderByIdDesc(student.getId(), IDENTITY_APPROVAL, "APPROVED")
                .or(() -> approvalRepo.findFirstByUserIdAndTypeOrderByIdDesc(student.getId(), IDENTITY_APPROVAL))
                .orElse(null);
    }

    private Job resolveJobForApplication(Application app) {
        if (app == null) return null;
        if (app.getJobId() != null) {
            Job job = jobRepo.findById(app.getJobId()).orElse(null);
            if (job != null) return job;
        }
        return findJobByIdText(app.getJobTitle(), app.getCompanyId());
    }

    private Job resolveJobForInternship(InternshipInfo internship, Application accepted) {
        Job job = resolveJobForApplication(accepted);
        if (job != null) return job;
        Long companyId = internship != null ? internship.getEnterpriseId() : accepted != null ? accepted.getCompanyId() : null;
        return findJobByIdText(internship != null ? internship.getPosition() : null, companyId);
    }

    private Job findJobByIdText(Object value, Long companyId) {
        String text = asString(value);
        if (text == null || !isPlainNumber(text)) return null;
        try {
            Job job = jobRepo.findById(Long.valueOf(text)).orElse(null);
            if (job != null && (companyId == null || Objects.equals(job.getCompanyId(), companyId))) return job;
        } catch (NumberFormatException ignored) {
            return null;
        }
        return null;
    }

    private boolean isBrokenText(String value) {
        if (value == null) return false;
        String text = value.trim();
        return text.matches("\\?{2,}") || text.contains("\\uFFFD");
    }

    private boolean isPlaceholderText(String value) {
        if (value == null) return false;
        String text = value.trim();
        return text.isEmpty()
                || "-".equals(text)
                || "Unfilled school".equals(text)
                || "Unfilled major".equals(text)
                || "Unlinked job".equals(text)
                || "Unbound school".equals(text)
                || text.startsWith("Company#");
    }

    private Archive anchorOnChain(String type, String name, String content) {
        return anchorOnChain(type, name, content, null, null);
    }

    private Archive anchorOnChain(String type, String name, String content, Long studentId, Long companyId) {
        return anchorOnChain(type, name, content, studentId, companyId, null, null);
    }

    private Archive anchorOnChain(String type, String name, String content, Long studentId, Long companyId,
                                  Long internshipId, String sourceId) {
        Archive archive = new Archive();
        archive.setType(type);
        archive.setName(name);
        archive.setStudentId(studentId);
        archive.setCompanyId(companyId);
        archive.setInternshipId(internshipId);
        archive.setSourceId(sourceId);
        return saveArchiveWithChainState(archive, content);
    }

    private Archive saveArchiveWithChainState(Archive archive, String content) {
        try {
            String hash = archive.getHash();
            if (hash == null || hash.isBlank()) {
                hash = BlockchainService.calculateHash(content + "|" + System.currentTimeMillis());
            }
            archive.setHash(hash);
            if (archive.getTime() == null || archive.getTime().isBlank()) {
                archive.setTime(LocalDateTime.now().toString().replace("T", " ").substring(0, 19));
            }
            archive.setChainStatus("LOCAL_FALLBACK");
            archive.setTxHash(null);
            archive.setBlock(null);
            archive.setChainError(null);
            try {
                String txHash = blockchainService.createCertificate(
                    System.currentTimeMillis() % 100000, System.currentTimeMillis() % 100000, hash,
                    blockchainService.getSchoolAccount(),
                    blockchainService.getEnterpriseAccount()
                );
                if (BlockchainService.isTransactionHash(txHash)) {
                    archive.setTxHash(txHash);
                    archive.setChainStatus("ON_CHAIN");
                    archive.setChainError(null);
                    archive.setBlock(System.currentTimeMillis() % 20000000L + 18400000L);
                } else {
                    archive.setChainError("WeBASE did not return a valid transactionHash: " + txHash);
                }
                log.info("Chain anchored: {} hash={} txHash={}", archive.getType(), hash, archive.getTxHash());
            } catch (Exception ce) {
                archive.setChainError(ce.getMessage());
                log.warn("Chain unavailable for {}, local hash: {}", archive.getType(), ce.getMessage());
            }
            Archive saved = archiveRepo.save(archive);
            log.info("Archive saved: {} {}", saved.getType(), saved.getName());
            return saved;
        } catch (Exception e) {
            log.warn("Failed to anchor {}: {}", archive.getType(), e.getMessage());
            archive.setChainStatus("LOCAL_FALLBACK");
            archive.setChainError(e.getMessage());
            return archiveRepo.save(archive);
        }
    }

    private Long asLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        String text = value.toString().trim();
        if (text.isEmpty()) return null;
        return Long.valueOf(text);
    }

    private Long firstLong(Object... values) {
        for (Object value : values) {
            Long parsed = asLong(value);
            if (parsed != null) return parsed;
        }
        return null;
    }

    private Double asDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.doubleValue();
        String text = value.toString().trim();
        if (text.isEmpty()) return null;
        return Double.valueOf(text);
    }

    private String asString(Object value) {
        if (value == null) return null;
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private String normalizeAttendance(Object value) {
        String text = asString(value);
        if (text == null || text.contains("?")) return "\u5168\u52e4";
        if ("\u5168\u52e4".equals(text) || "\u7f3a\u52e41-2\u5929".equals(text) || "\u7f3a\u52e43\u5929\u4ee5\u4e0a".equals(text)) return text;
        if (text.contains("1") || text.contains("2")) return "\u7f3a\u52e41-2\u5929";
        if (text.contains("3")) return "\u7f3a\u52e43\u5929\u4ee5\u4e0a";
        return "\u5168\u52e4";
    }

    private LocalDate asDate(Object value) {
        String text = asString(value);
        return text == null ? null : LocalDate.parse(text);
    }

    private Map<String,Object> mapInternship(InternshipInfo i) {
        Map<String,Object> m = new HashMap<>();
        User student = i.getStudentId() != null ? userRepo.findById(i.getStudentId()).orElse(null) : null;
        Application accepted = i.getStudentId() != null && i.getEnterpriseId() != null
                ? appRepo.findByStudentIdAndCompanyIdAndStatus(i.getStudentId(), i.getEnterpriseId(), "accepted").stream()
                        .max(Comparator.comparing(Application::getId)).orElse(null)
                : null;
        Job job = resolveJobForInternship(i, accepted);
        m.put("id", i.getId());
        m.put("studentId", i.getStudentId());
        m.put("studentName", firstDisplayText(student != null ? student.getRealName() : null, i.getStudentName(), accepted != null ? accepted.getName() : null, student != null ? student.getUsername() : null));
        m.put("studentIdCard", i.getStudentIdCard());
        m.put("enterpriseId", i.getEnterpriseId());
        m.put("companyId", i.getEnterpriseId());
        m.put("enterpriseName", i.getEnterpriseName());
        m.put("enterpriseCode", i.getEnterpriseCode());
        m.put("schoolId", i.getSchoolId());
        m.put("schoolName", studentSchoolName(student, i.getSchoolName(), accepted != null ? accepted.getSchool() : null));
        m.put("major", studentMajor(student, accepted != null ? accepted.getMajor() : null));
        m.put("jobId", accepted != null && accepted.getJobId() != null ? accepted.getJobId() : job != null ? job.getId() : null);
        m.put("jobTitle", jobDisplayTitle(job, accepted != null ? accepted.getJobTitle() : null, i.getPosition()));
        m.put("position", jobDisplayTitle(job, accepted != null ? accepted.getJobTitle() : null, i.getPosition()));
        m.put("startDate", i.getStartDate() != null ? i.getStartDate().toString() : null);
        m.put("endDate", i.getEndDate() != null ? i.getEndDate().toString() : null);
        m.put("department", i.getDepartment());
        m.put("mentorName", i.getMentorName());
        m.put("mentorPhone", i.getMentorPhone());
        m.put("description", i.getDescription());
        m.put("status", i.getStatus());
        m.put("contentHash", i.getContentHash());
        m.put("blockchainTxHash", i.getBlockchainTxHash());
        m.put("agreementSigned", Boolean.TRUE.equals(i.getAgreementSigned()));
        m.put("agreementSignedAt", i.getAgreementSignedAt());
        return m;
    }

    private Map<String,Object> mapAssessment(Assessment a) {
        Map<String,Object> m = new HashMap<>();
        m.put("id", a.getId());
        m.put("internshipId", a.getInternshipId());
        m.put("studentId", a.getStudentId());
        m.put("companyId", a.getCompanyId());
        m.put("company", enterpriseDisplayName(a.getCompanyId()));
        m.put("student", a.getStudent());
        m.put("month", a.getMonth());
        m.put("attendance", normalizeAttendance(a.getAttendance()));
        m.put("score", a.getScore());
        m.put("comment", a.getComment());
        m.put("status", a.getStatus());
        return m;
    }

    private Map<String,Object> mapDispute(Dispute d) {
        Map<String,Object> m = new HashMap<>();
        Assessment assessment = d.getAssessmentId() != null ? assessRepo.findById(d.getAssessmentId()).orElse(null) : null;
        m.put("id", d.getId());
        m.put("assessmentId", d.getAssessmentId());
        m.put("internshipId", assessment != null ? assessment.getInternshipId() : null);
        m.put("studentId", d.getStudentId());
        m.put("companyId", d.getCompanyId());
        m.put("company", enterpriseDisplayName(d.getCompanyId()));
        m.put("student", d.getStudent());
        m.put("reason", firstDisplayText(d.getReason(), disputeReasonFallback(assessment)));
        m.put("date", d.getDate());
        m.put("status", d.getStatus());
        m.put("opinion", firstDisplayText(d.getOpinion()));
        m.put("resolvedDate", d.getResolvedDate());
        return m;
    }

    private String disputeReasonFallback(Assessment assessment) {
        if (assessment == null) return "Dispute reason pending";
        String month = firstDisplayText(assessment.getMonth(), "鏈");
        String score = assessment.getScore() != null ? " (" + assessment.getScore() + ")" : "";
        return "Dispute for " + month + " assessment" + score;
    }

    private Map<String,Object> mapReport(Report r) {
        Map<String,Object> m = new HashMap<>();
        m.put("id", r.getId());
        m.put("internshipId", r.getInternshipId());
        m.put("studentId", r.getStudentId());
        m.put("enterpriseId", r.getEnterpriseId());
        m.put("studentName", r.getStudentName());
        m.put("enterpriseName", r.getEnterpriseName());
        m.put("title", r.getTitle());
        m.put("content", r.getContent());
        m.put("date", r.getDate());
        m.put("hours", r.getHours());
        m.put("submitted", r.getSubmitted());
        return m;
    }

    private Map<String,Object> mapNotice(Notice n) {
        Map<String,Object> m = new HashMap<>();
        m.put("id", n.getId());
        m.put("title", n.getTitle());
        m.put("content", n.getContent());
        m.put("from", n.getFromName());
        m.put("date", n.getDate());
        m.put("type", n.getType());
        m.put("schoolId", n.getSchoolId());
        m.put("schoolName", n.getSchoolId() != null ? resolveSchoolName(n.getSchoolId(), null) : null);
        return m;
    }

    private Map<String,Object> mapMessage(PlatformMessage message) {
        Map<String,Object> m = new HashMap<>();
        m.put("id", message.getId());
        m.put("title", message.getTitle());
        m.put("content", message.getContent());
        m.put("from", message.getFromName());
        m.put("date", message.getDate());
        m.put("schoolId", message.getSchoolId());
        m.put("schoolName", message.getSchoolId() != null ? resolveSchoolName(message.getSchoolId(), null) : null);
        m.put("read", Boolean.TRUE.equals(message.getReadFlag()));
        return m;
    }

    private Map<String,Object> mapApproval(Approval approval) {
        Map<String,Object> m = new HashMap<>();
        m.put("id", approval.getId());
        m.put("userId", approval.getUserId());
        m.put("type", approval.getType());
        m.put("name", approval.getName());
        m.put("code", approval.getCode());
        m.put("school", approval.getSchool());
        m.put("major", approval.getMajor());
        m.put("contact", approval.getContact());
        m.put("phone", approval.getPhone());
        m.put("status", approval.getStatus());
        m.put("date", approval.getDate());
        return m;
    }
}

