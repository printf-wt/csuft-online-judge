import { computed, ref } from 'vue'

const LOCALE_KEY = 'csuft_oj_locale'
const supportedLocales = ['zh-CN', 'en']
const initialLocale = localStorage.getItem(LOCALE_KEY)
const locale = ref(supportedLocales.includes(initialLocale) ? initialLocale : 'zh-CN')

const messages = {
  'zh-CN': {
    common: {
      home: '首页', problems: '题库', ranklist: '排行榜', contests: '比赛', submissions: '提交记录',
      login: '登录', register: '注册', logout: '退出登录', profile: '个人中心', admin: '管理监控',
      refresh: '刷新', refreshNow: '立即刷新', action: '操作', status: '状态', score: '分数', language: '语言',
      copy: '复制', copied: '已复制', reset: '重置', time: '时间', memory: '内存', user: '用户', rank: '排名',
      easy: '简单', medium: '中等', hard: '困难', upcoming: '未开始', running: '进行中', ended: '已结束',
      noDescription: '暂无描述。', notProvided: '未提供', less: '少', more: '多', total: '总分',
      dataLoadFailed: '数据加载失败，请稍后重试。',
    },
    layout: {
      slogan: '编程、竞赛、突破。', profile: '个人中心', mySubmissions: '我的提交', admin: '管理监控', userManagement: '用户管理',
      problemManagement: '题目管理', contestManagement: '竞赛管理',
      footer: 'CSUFT OJ - 为无畏的练习者而生', language: '语言', chinese: '中文', english: 'English',
    },
    home: {
      title: '让每一次提交，都磨炼出更敏锐的直觉。',
      description: '面向日常练习、课程项目和竞赛训练的现代在线评测系统。',
      asyncJudge: '异步评测', contestMode: '竞赛模式', scoreMode: '计分模式', pinnedNotice: '置顶通知',
      noticeHint: '系统公告', noNotices: '暂无公告。', today: '今日建议', todayHint: '先用一道简单题热身，再向更难的问题进发。',
    },
    auth: {
      welcomeBack: '欢迎回来', loginTitle: '登录并延续你的刷题节奏。',
      loginDescription: '继续解题、参加比赛，不断磨炼你的算法直觉。', username: '用户名', password: '密码',
      usernamePlaceholder: '3-32 个字符', noAccount: '还没有账号？', createOne: '立即注册',
      startTraining: '开始训练', registerTitle: '创建你的 OJ 身份。',
      registerDescription: '注册后即可记录提交、通过数量、比赛成绩和练习热力图。', email: '邮箱',
      emailCode: '邮箱验证码', emailCodePlaceholder: '6 位验证码', sendCode: '发送验证码', resendCodeIn: '{seconds}s 后重发',
      confirmPassword: '确认密码', createAccount: '创建账号', hasAccount: '已有账号？',
      enterUsername: '请输入用户名', usernameLength: '用户名长度必须为 3-32 个字符', enterPassword: '请输入密码',
      passwordLength: '密码长度必须为 8-128 个字符', enterEmail: '请输入邮箱', validEmail: '请输入有效的邮箱地址',
      enterEmailCode: '请输入邮箱验证码', emailCodeLength: '验证码必须为 6 位数字', emailCodeSent: '验证码已发送，请查收邮箱。',
      emailCodeSendFailed: '验证码发送失败，请稍后重试。',
      confirmPasswordRequired: '请确认密码', passwordMismatch: '两次输入的密码不一致', loginSuccess: '登录成功',
      registerSuccess: '注册成功，请登录。',
      loginFailed: '登录失败，请检查用户名和密码。', registerFailed: '注册失败，请检查输入信息。',
      nickname: '昵称',
    },
    problem: {
      archive: '题目归档', title: '选择一道值得较量的题目。', description: '搜索、筛选，然后进入在线编程页面开始挑战。',
      matched: '匹配题目', search: '按标题或 ID 搜索...', difficulty: '难度', tags: '算法标签', titleColumn: '标题',
      passRate: '通过率', solve: '作答', detailTitle: '题目 #{id}', protectedDetail: '受保护的题目详情页。', startSolving: '开始作答',
      input: '输入', output: '输出', sampleInput: '样例输入', sampleOutput: '样例输出', submitCode: '提交代码',
      judging: '评测中...', submission: '提交 #{id}', judgeRunning: '评测机正在编译并运行你的代码。', judgeCompleted: '评测完成。',
      codeEmpty: '代码不能为空', submitted: '已提交 #{id}', loadingTitle: '题目加载中...', loadingStatement: '题目描述加载中。',
      inputDescription: '输入说明。', outputDescription: '输出说明。', noStatement: '暂无题目描述。', noInput: '暂无输入说明。', noOutput: '暂无输出说明。',
      contestNumber: '比赛 #{id}', problemNumber: '题目 #{id}',
      judgeTimeout: '评测超时，请重试。', submitFailed: '提交失败，请重试。', loadFailed: '题库加载失败，请稍后重试。',
    },
    contest: {
      arena: '竞赛场', title: '选择你的下一场比赛。', description: '蓝色代表未开始，绿色代表进行中，灰色代表已结束。比赛开始后，时间与你一同流逝。',
      remaining: '剩余 {time}', fallbackDescription: '暂无描述。', problems: '题目', alias: '编号', problemId: '题目 ID',
      solveAlias: '作答 {alias}', standings: '排名', penalty: '罚时', accepted: '通过数', submittedAt: '提交时间',
      liveDescription: '比赛题目与实时排名。', loading: '比赛加载中...', problem: '题目',
      register: '报名参赛', registered: '已报名', registerSuccess: '报名成功', registerFailed: '报名失败，请稍后重试',
      loadFailed: '比赛数据加载失败，请稍后重试。',
    },
    profile: {
      center: '个人中心', description: '查看身份、进度与每日练习节奏。', account: '账户信息', studentNo: '学号',
      email: '邮箱', role: '角色', globalAc: '全站通过', totalSubmits: '总提交数', heatmap: '提交热力图',
      heatmapDescription: '最近 365 天的练习强度。', heatmapAria: '365 天提交热力图', submissionsCount: '{date}：{count} 次提交',
      defaultCoder: 'CSUFT 编程者', edit: '编辑资料', nickname: '昵称', joinedAt: '加入时间', cancel: '取消', save: '保存',
      nicknameRequired: '请输入昵称', saved: '个人资料已更新', changePassword: '修改密码', currentPassword: '当前密码',
      newPassword: '新密码', currentPasswordRequired: '请输入当前密码', newPasswordRequired: '请输入新密码', confirmChange: '确认修改',
      passwordSecurityHint: '修改成功后，所有设备上的登录会话都会立即失效。', passwordChanged: '密码已修改，请重新登录。', passwordChangeFailed: '密码修改失败',
    },
    admin: {
      observatory: '管理观测台', title: '安全审计与系统实时遥测。',
      description: '实时查看 JVM 内存、CPU 压力、评测队列积压和敏感操作。', jvmUsed: 'JVM 已用内存', max: '最大 {value}',
      systemCpu: '系统 CPU', process: '进程 {value}', judgeQueue: '评测队列', waiting: '等待中的提交',
      memoryCurve: 'JVM 内存曲线', queueBacklog: '评测队列积压', auditLog: '审计日志',
      auditDescription: '由 @AuditLog 捕获的控制器操作。', filterAction: '筛选操作', operator: '操作人', ip: 'IP',
      target: '目标', detail: '详情', usedMemory: '已用内存', queueSize: '队列长度',
      judgeWorkers: '评测线程', queueUsage: '队列使用率 {value}', uptime: '运行时长 {value}',
      monitorUnavailable: '实时监控暂不可用', monitorRequestFailed: '无法连接系统监控接口', auditRequestFailed: '审计日志加载失败',
      noticeManagement: '公告管理', noticeManagementDescription: '发布、置顶以及控制主页公告可见性。', createNotice: '发布公告', editNotice: '编辑公告',
      noticeTitle: '标题', noticeContent: '内容', pinned: '置顶', visible: '可见', yes: '是', no: '否', edit: '编辑', delete: '删除',
      noticeRequestFailed: '公告列表加载失败', noticeRequired: '请填写公告标题和内容', noticeSaved: '公告已保存',
      deleteNotice: '删除公告', deleteNoticeConfirm: '确认删除“{title}”吗？', noticeDeleted: '公告已删除',
      usersKicker: '账号治理', usersTitle: '用户与权限管理', usersDescription: '检索用户、调整角色与账号状态，权限变更会立即注销该用户的全部会话。',
      searchUsers: '搜索用户名、昵称或邮箱', filterRole: '筛选角色', filterStatus: '筛选状态', search: '搜索', active: '正常', disabled: '已禁用',
      editUser: '编辑用户权限', sessionResetHint: '角色或状态发生变化时，该用户需要重新登录。', usersRequestFailed: '用户列表加载失败', userSaved: '用户权限已更新', userSaveFailed: '用户权限更新失败',
    },
    management: {
      problemKicker: '内容工作台', problemTitle: '题目管理', problemDescription: '创建题目、批量导入 JSON，并为每道题上传评测测试点。',
      createProblem: '新建题目', editProblem: '编辑题目', importJson: '导入 JSON', visibility: '可见性', public: '公开', hidden: '隐藏', private: '私有',
      limits: '时空限制', submissionStats: '通过 / 提交', preview: '预览', testcases: '测试点', problemName: '题目名称', statement: '题目描述',
      timeLimit: '时间限制（ms）', memoryLimit: '内存限制（KB）', titleRequired: '请输入题目名称', statementRequired: '请输入题目描述', difficultyRequired: '请选择难度',
      problemLoadFailed: '题目列表加载失败', problemSaved: '题目已保存', problemSaveFailed: '题目保存失败', deleteProblem: '删除题目',
      deleteProblemConfirm: '确认删除题目“{title}”吗？已有提交或比赛引用时数据库可能拒绝删除。', problemDeleted: '题目已删除', problemDeleteFailed: '题目删除失败',
      uploadTitle: '上传题目 #{id} 测试点', zipHint: 'ZIP 根目录中只能包含成对的测试文件，例如 1.in 与 1.out。重新上传会替换原测试点。',
      dropZip: '拖放 ZIP 到此处，或点击选择文件', upload: '上传', testcasesUploaded: '已导入 {count} 组测试点', testcaseUploadFailed: '测试点上传失败',
      emptyImport: 'JSON 中没有可导入的题目', importResult: '导入完成：成功 {succeeded}，失败 {failed}', importFailed: '题目 JSON 导入失败',
      contestKicker: '竞赛工作台', contestTitle: '竞赛管理', contestDescription: '发布和维护 ACM/IOI 竞赛，并编排竞赛题目。', createContest: '发布竞赛',
      editContest: '编辑竞赛', contestName: '竞赛名称', contestIntro: '竞赛说明', rule: '赛制', period: '比赛时间', startTime: '开始时间', endTime: '结束时间',
      selectTime: '选择日期和时间', contestTitleRequired: '请输入竞赛名称', ruleRequired: '请选择赛制', startRequired: '请选择开始时间', endRequired: '请选择结束时间',
      endAfterStart: '结束时间必须晚于开始时间', contestLoadFailed: '竞赛列表加载失败', contestSaved: '竞赛已保存', contestSaveFailed: '竞赛保存失败',
      deleteContest: '删除竞赛', deleteContestConfirm: '确认删除竞赛“{title}”吗？报名和题目编排数据也会删除。', contestDeleted: '竞赛已删除', contestDeleteFailed: '竞赛删除失败',
      bindProblems: '编排题目', bindTitle: '编排“{title}”的题目', bindingHint: '列表顺序即比赛展示顺序；别名和题目不能重复。保存会整体替换原编排。',
      selectProblem: '选择题目', alias: '别名', addProblem: '添加题目', saveBindings: '保存编排', bindingRequired: '请至少添加一道题并完整填写',
      bindingDuplicate: '题目或别名不能重复', bindingLoadFailed: '竞赛题目加载失败', bindingsSaved: '竞赛题目编排已保存', bindingSaveFailed: '竞赛题目保存失败',
    },
    ranklist: {
      title: '全局排行榜', description: '按通过题数降序、提交次数升序排列。',
      rank: '排名', solved: '通过数', submissions: '提交数',
    },
    submissions: {
      title: '提交记录', description: '查看所有提交记录及评测状态。',
      problem: '题目', language: '语言', status: '状态', time: '提交时间',
      filterStatus: '筛选状态', filterLanguage: '筛选语言',
    },
    placeholder: {
      ranklist: '全局排行榜', ranklistDescription: '全局排行榜占位页，可在接口就绪后接入 `/api/users/ranklist`。',
      submissions: '提交记录', submissionsDescription: '受保护的提交历史页面。',
    },
    route: {
      home: '首页', problems: '题库', ranklist: '排行榜', login: '登录', register: '注册', profile: '个人中心',
      admin: '管理监控', adminUsers: '用户管理', manageProblems: '题目管理', manageContests: '竞赛管理', problemDetail: '题目详情', solve: '在线作答', submissions: '提交记录', contests: '比赛',
      contestDetail: '比赛详情', contestPlayground: '比赛作答',
    },
  },
  en: {
    common: {
      home: 'Home', problems: 'Problems', ranklist: 'Ranklist', contests: 'Contests', submissions: 'Submissions',
      login: 'Login', register: 'Register', logout: 'Logout', profile: 'Profile', admin: 'Admin monitor',
      refresh: 'Refresh', refreshNow: 'Refresh now', action: 'Action', status: 'Status', score: 'Score', language: 'Language',
      copy: 'Copy', copied: 'Copied', reset: 'Reset', time: 'Time', memory: 'Memory', user: 'User', rank: 'Rank',
      easy: 'Easy', medium: 'Medium', hard: 'Hard', upcoming: 'Upcoming', running: 'Running', ended: 'Ended',
      noDescription: 'No description yet.', notProvided: 'Not provided', less: 'Less', more: 'More', total: 'Total',
      dataLoadFailed: 'Failed to load data. Please try again later.',
    },
    layout: {
      slogan: 'Code, compete, conquer.', profile: 'Profile', mySubmissions: 'My submissions', admin: 'Admin monitor', userManagement: 'User management',
      problemManagement: 'Problem management', contestManagement: 'Contest management',
      footer: 'CSUFT OJ - Built for fearless practice', language: 'Language', chinese: '中文', english: 'English',
    },
    home: {
      title: 'Turn each submission into sharper instinct.',
      description: 'A modern online judge for daily practice, course projects, and contest training.',
      asyncJudge: 'Async judge', contestMode: 'Contest mode', scoreMode: 'Score mode', pinnedNotice: 'Pinned notice',
      noticeHint: 'System notices', noNotices: 'No notices yet.', today: 'Today', todayHint: 'Warm up with one easy problem, then go hunting.',
    },
    auth: {
      welcomeBack: 'Welcome back', loginTitle: 'Sign in and keep the streak alive.',
      loginDescription: 'Continue solving problems, joining contests, and sharpening your algorithm instincts.', username: 'Username', password: 'Password',
      usernamePlaceholder: '3-32 characters', noAccount: 'No account yet?', createOne: 'Create one',
      startTraining: 'Start training', registerTitle: 'Create your judge identity.',
      registerDescription: 'Register once, then your submissions, AC count, contests, and practice heatmap can grow from here.', email: 'Email',
      emailCode: 'Email code', emailCodePlaceholder: '6-digit code', sendCode: 'Send code', resendCodeIn: 'Resend in {seconds}s',
      confirmPassword: 'Confirm password', createAccount: 'Create account', hasAccount: 'Already have an account?',
      enterUsername: 'Please enter username', usernameLength: 'Username length must be 3 to 32 characters', enterPassword: 'Please enter password',
      passwordLength: 'Password length must be 8 to 128 characters', enterEmail: 'Please enter email', validEmail: 'Please enter a valid email address',
      enterEmailCode: 'Please enter the email code', emailCodeLength: 'The code must be 6 digits', emailCodeSent: 'Verification code sent. Please check your inbox.',
      emailCodeSendFailed: 'Failed to send verification code. Please try again later.',
      confirmPasswordRequired: 'Please confirm password', passwordMismatch: 'The two passwords do not match', loginSuccess: 'Login successful',
      registerSuccess: 'Registration successful. Please login.',
      loginFailed: 'Login failed. Please check your username and password.', registerFailed: 'Registration failed. Please check your input.',
      nickname: 'Nickname',
    },
    problem: {
      archive: 'Problem archive', title: 'Choose a problem worth wrestling with.', description: 'Search, filter, and jump into the playground when a problem starts whispering your name.',
      matched: 'matched', search: 'Search by title or ID...', difficulty: 'Difficulty', tags: 'Algorithm tags', titleColumn: 'Title',
      passRate: 'Pass rate', solve: 'Solve', detailTitle: 'Problem #{id}', protectedDetail: 'Protected problem detail page.', startSolving: 'Start solving',
      input: 'Input', output: 'Output', sampleInput: 'Sample input', sampleOutput: 'Sample output', submitCode: 'Submit code',
      judging: 'Judging...', submission: 'Submission #{id}', judgeRunning: 'The judge is compiling and running your code.', judgeCompleted: 'Judge completed.',
      codeEmpty: 'Code cannot be empty', submitted: 'Submitted #{id}', loadingTitle: 'Loading problem...', loadingStatement: 'Problem statement is loading.',
      inputDescription: 'Input description.', outputDescription: 'Output description.', noStatement: 'No statement yet.', noInput: 'No input description.', noOutput: 'No output description.',
      contestNumber: 'Contest #{id}', problemNumber: 'Problem #{id}',
      judgeTimeout: 'Judge timed out. Please try again.', submitFailed: 'Submission failed. Please try again.', loadFailed: 'Failed to load problems. Please try again later.',
    },
    contest: {
      arena: 'Arena', title: 'Pick your next contest window.', description: 'Blue for upcoming, green for live, gray for finished. When the arena is live, the clock ticks with you.',
      remaining: 'Remaining {time}', fallbackDescription: 'No description yet.', problems: 'Problems', alias: 'Alias', problemId: 'Problem ID',
      solveAlias: 'Solve {alias}', standings: 'Standings', penalty: 'Penalty', accepted: 'AC', submittedAt: 'Submitted at',
      liveDescription: 'Contest problems and live standings.', loading: 'Loading contest...', problem: 'Problem',
      register: 'Register', registered: 'Registered', registerSuccess: 'Registration successful', registerFailed: 'Registration failed. Please try again.',
      loadFailed: 'Failed to load contest data. Please try again later.',
    },
    profile: {
      center: 'Personal center', description: 'A compact dashboard for identity, progress, and daily practice rhythm.', account: 'Account', studentNo: 'Student No.',
      email: 'Email', role: 'Role', globalAc: 'Global AC', totalSubmits: 'Total submits', heatmap: 'Submission heatmap',
      heatmapDescription: 'Recent 365 days of practice intensity.', heatmapAria: '365 day submission heatmap', submissionsCount: '{date}: {count} submissions',
      defaultCoder: 'CSUFT Coder', edit: 'Edit profile', nickname: 'Nickname', joinedAt: 'Joined', cancel: 'Cancel', save: 'Save',
      nicknameRequired: 'Please enter a nickname', saved: 'Profile updated', changePassword: 'Change password', currentPassword: 'Current password',
      newPassword: 'New password', currentPasswordRequired: 'Please enter your current password', newPasswordRequired: 'Please enter a new password', confirmChange: 'Change password',
      passwordSecurityHint: 'After the password changes, sessions on every device will be invalidated immediately.', passwordChanged: 'Password changed. Please sign in again.', passwordChangeFailed: 'Failed to change password',
    },
    admin: {
      observatory: 'Admin observatory', title: 'Security audit and live system telemetry.',
      description: 'Watch JVM memory, CPU pressure, judge queue backlog, and sensitive operations as they happen.', jvmUsed: 'JVM Used', max: 'Max {value}',
      systemCpu: 'System CPU', process: 'Process {value}', judgeQueue: 'Judge Queue', waiting: 'Waiting submissions',
      memoryCurve: 'JVM memory curve', queueBacklog: 'Judge queue backlog', auditLog: 'Audit log',
      auditDescription: 'Controller operations captured by @AuditLog.', filterAction: 'Filter action', operator: 'Operator', ip: 'IP',
      target: 'Target', detail: 'Detail', usedMemory: 'Used memory', queueSize: 'Queue size',
      judgeWorkers: 'Judge workers', queueUsage: 'Queue usage {value}', uptime: 'Uptime {value}',
      monitorUnavailable: 'Live telemetry unavailable', monitorRequestFailed: 'Unable to reach the monitor API', auditRequestFailed: 'Failed to load audit logs',
      noticeManagement: 'Notice management', noticeManagementDescription: 'Publish, pin, and control homepage notice visibility.', createNotice: 'Create notice', editNotice: 'Edit notice',
      noticeTitle: 'Title', noticeContent: 'Content', pinned: 'Pinned', visible: 'Visible', yes: 'Yes', no: 'No', edit: 'Edit', delete: 'Delete',
      noticeRequestFailed: 'Failed to load notices', noticeRequired: 'Notice title and content are required', noticeSaved: 'Notice saved',
      deleteNotice: 'Delete notice', deleteNoticeConfirm: 'Delete "{title}"?', noticeDeleted: 'Notice deleted',
      usersKicker: 'Account governance', usersTitle: 'Users and access', usersDescription: 'Search users and manage roles or account status. Access changes invalidate every active session immediately.',
      searchUsers: 'Search username, nickname, or email', filterRole: 'Filter role', filterStatus: 'Filter status', search: 'Search', active: 'Active', disabled: 'Disabled',
      editUser: 'Edit user access', sessionResetHint: 'Changing the role or status requires this user to sign in again.', usersRequestFailed: 'Failed to load users', userSaved: 'User access updated', userSaveFailed: 'Failed to update user access',
    },
    management: {
      problemKicker: 'Content workspace', problemTitle: 'Problem management', problemDescription: 'Create problems, import JSON batches, and upload judge test cases.',
      createProblem: 'Create problem', editProblem: 'Edit problem', importJson: 'Import JSON', visibility: 'Visibility', public: 'Public', hidden: 'Hidden', private: 'Private',
      limits: 'Limits', submissionStats: 'Accepted / Submitted', preview: 'Preview', testcases: 'Test cases', problemName: 'Problem title', statement: 'Statement',
      timeLimit: 'Time limit (ms)', memoryLimit: 'Memory limit (KB)', titleRequired: 'Enter a problem title', statementRequired: 'Enter the problem statement', difficultyRequired: 'Select a difficulty',
      problemLoadFailed: 'Failed to load problems', problemSaved: 'Problem saved', problemSaveFailed: 'Failed to save problem', deleteProblem: 'Delete problem',
      deleteProblemConfirm: 'Delete "{title}"? The database may reject deletion when submissions or contests still reference it.', problemDeleted: 'Problem deleted', problemDeleteFailed: 'Failed to delete problem',
      uploadTitle: 'Upload test cases for problem #{id}', zipHint: 'The ZIP root may contain only paired files such as 1.in and 1.out. Uploading again replaces existing cases.',
      dropZip: 'Drop a ZIP here or click to select', upload: 'Upload', testcasesUploaded: 'Imported {count} test case pairs', testcaseUploadFailed: 'Failed to upload test cases',
      emptyImport: 'The JSON contains no problems', importResult: 'Import complete: {succeeded} succeeded, {failed} failed', importFailed: 'Failed to import problem JSON',
      contestKicker: 'Contest workspace', contestTitle: 'Contest management', contestDescription: 'Publish and maintain ACM/IOI contests and arrange their problems.', createContest: 'Publish contest',
      editContest: 'Edit contest', contestName: 'Contest title', contestIntro: 'Description', rule: 'Rule', period: 'Contest period', startTime: 'Start time', endTime: 'End time',
      selectTime: 'Select date and time', contestTitleRequired: 'Enter a contest title', ruleRequired: 'Select a rule', startRequired: 'Select a start time', endRequired: 'Select an end time',
      endAfterStart: 'End time must be after start time', contestLoadFailed: 'Failed to load contests', contestSaved: 'Contest saved', contestSaveFailed: 'Failed to save contest',
      deleteContest: 'Delete contest', deleteContestConfirm: 'Delete "{title}"? Registrations and problem bindings will also be removed.', contestDeleted: 'Contest deleted', contestDeleteFailed: 'Failed to delete contest',
      bindProblems: 'Arrange problems', bindTitle: 'Arrange problems for "{title}"', bindingHint: 'The list order is the display order. Aliases and problems must be unique. Saving replaces the previous arrangement.',
      selectProblem: 'Select problem', alias: 'Alias', addProblem: 'Add problem', saveBindings: 'Save arrangement', bindingRequired: 'Add at least one complete problem row',
      bindingDuplicate: 'Problems and aliases must be unique', bindingLoadFailed: 'Failed to load contest problems', bindingsSaved: 'Contest problems saved', bindingSaveFailed: 'Failed to save contest problems',
    },
    ranklist: {
      title: 'Global Ranklist', description: 'Ordered by solved count descending, submit count ascending.',
      rank: 'Rank', solved: 'Solved', submissions: 'Submissions',
    },
    submissions: {
      title: 'Submissions', description: 'View all submission records and judge status.',
      problem: 'Problem', language: 'Language', status: 'Status', time: 'Submitted at',
      filterStatus: 'Filter status', filterLanguage: 'Filter language',
    },
    placeholder: {
      ranklist: 'Ranklist', ranklistDescription: 'Global ranking placeholder. Connect `/api/users/ranklist` when ready.',
      submissions: 'Submissions', submissionsDescription: 'Protected submission history page.',
    },
    route: {
      home: 'Home', problems: 'Problems', ranklist: 'Ranklist', login: 'Login', register: 'Register', profile: 'Profile',
      admin: 'Admin Monitor', adminUsers: 'User Management', manageProblems: 'Problem Management', manageContests: 'Contest Management', problemDetail: 'Problem Detail', solve: 'Solve', submissions: 'Submissions', contests: 'Contests',
      contestDetail: 'Contest Detail', contestPlayground: 'Contest Playground',
    },
  },
}

function resolveMessage(key) {
  return key.split('.').reduce((value, part) => value?.[part], messages[locale.value])
}

export function t(key, params = {}) {
  const template = resolveMessage(key) ?? key
  return Object.entries(params).reduce(
    (text, [name, value]) => text.replaceAll(`{${name}}`, String(value)),
    String(template),
  )
}

export function setLocale(value) {
  if (!supportedLocales.includes(value)) return
  locale.value = value
  localStorage.setItem(LOCALE_KEY, value)
  document.documentElement.lang = value
}

export function formatDateTime(value) {
  return value ? new Date(value).toLocaleString(locale.value) : '-'
}

export function useI18n() {
  return {
    locale,
    isChinese: computed(() => locale.value === 'zh-CN'),
    t,
    setLocale,
    formatDateTime,
  }
}

setLocale(locale.value)
