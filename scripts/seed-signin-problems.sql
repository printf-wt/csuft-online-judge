SET NAMES utf8mb4;

INSERT INTO tb_problem (title, description, input_description, output_description, sample_input, sample_output, difficulty, time_limit_ms, memory_limit_kb, author_id, is_visible, accepted_count, submit_count, created_at, updated_at)
SELECT '[签到] A+B', '输入两个整数，输出它们的和。', '一行两个整数 a 和 b。', '输出 a+b 的结果。', '1 2', '3', 'EASY', 1000, 131072, 2, 1, 0, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM tb_problem WHERE title='[签到] A+B');

INSERT INTO tb_problem (title, description, input_description, output_description, sample_input, sample_output, difficulty, time_limit_ms, memory_limit_kb, author_id, is_visible, accepted_count, submit_count, created_at, updated_at)
SELECT '[签到] 两数之差', '输入两个整数，输出第一个整数减去第二个整数的结果。', '一行两个整数 a 和 b。', '输出 a-b。', '10 3', '7', 'EASY', 1000, 131072, 2, 1, 0, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM tb_problem WHERE title='[签到] 两数之差');

INSERT INTO tb_problem (title, description, input_description, output_description, sample_input, sample_output, difficulty, time_limit_ms, memory_limit_kb, author_id, is_visible, accepted_count, submit_count, created_at, updated_at)
SELECT '[签到] 两数之积', '输入两个整数，输出它们的乘积。', '一行两个整数 a 和 b。', '输出 a*b。', '6 7', '42', 'EASY', 1000, 131072, 2, 1, 0, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM tb_problem WHERE title='[签到] 两数之积');

INSERT INTO tb_problem (title, description, input_description, output_description, sample_input, sample_output, difficulty, time_limit_ms, memory_limit_kb, author_id, is_visible, accepted_count, submit_count, created_at, updated_at)
SELECT '[签到] 较大的数', '输入两个整数，输出其中较大的一个；若两数相等则输出任意一个。', '一行两个整数 a 和 b。', '输出 max(a,b)。', '8 5', '8', 'EASY', 1000, 131072, 2, 1, 0, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM tb_problem WHERE title='[签到] 较大的数');

INSERT INTO tb_problem (title, description, input_description, output_description, sample_input, sample_output, difficulty, time_limit_ms, memory_limit_kb, author_id, is_visible, accepted_count, submit_count, created_at, updated_at)
SELECT '[签到] 奇偶判断', '输入一个整数。若它是偶数输出 EVEN，否则输出 ODD。', '一行一个整数 n。', '输出 EVEN 或 ODD。', '12', 'EVEN', 'EASY', 1000, 131072, 2, 1, 0, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM tb_problem WHERE title='[签到] 奇偶判断');

INSERT INTO tb_problem (title, description, input_description, output_description, sample_input, sample_output, difficulty, time_limit_ms, memory_limit_kb, author_id, is_visible, accepted_count, submit_count, created_at, updated_at)
SELECT '[签到] 温度转换', '给定整数摄氏温度 C，按 F=C*9/5+32 计算并输出整数华氏温度。测试数据保证 C*9 能被 5 整除。', '一行一个整数 C。', '输出对应的整数华氏温度 F。', '20', '68', 'EASY', 1000, 131072, 2, 1, 0, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM tb_problem WHERE title='[签到] 温度转换');

INSERT INTO tb_problem (title, description, input_description, output_description, sample_input, sample_output, difficulty, time_limit_ms, memory_limit_kb, author_id, is_visible, accepted_count, submit_count, created_at, updated_at)
SELECT '[签到] 转为大写', '输入一个仅由英文字母组成的单词，将所有小写字母转换为大写后输出。', '一行一个不含空格的英文单词。', '输出转换后的大写单词。', 'Hello', 'HELLO', 'EASY', 1000, 131072, 2, 1, 0, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM tb_problem WHERE title='[签到] 转为大写');

INSERT INTO tb_problem (title, description, input_description, output_description, sample_input, sample_output, difficulty, time_limit_ms, memory_limit_kb, author_id, is_visible, accepted_count, submit_count, created_at, updated_at)
SELECT '[签到] 反转字符串', '输入一个不含空格的字符串，输出其反转结果。', '一行一个字符串 s。', '输出反转后的字符串。', 'csuft', 'tfusc', 'EASY', 1000, 131072, 2, 1, 0, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM tb_problem WHERE title='[签到] 反转字符串');

INSERT INTO tb_problem (title, description, input_description, output_description, sample_input, sample_output, difficulty, time_limit_ms, memory_limit_kb, author_id, is_visible, accepted_count, submit_count, created_at, updated_at)
SELECT '[签到] N 个整数求和', '给定 N 个整数，输出它们的总和。', '第一行一个整数 N；第二行 N 个整数。', '输出这 N 个整数的和。', '5\n1 2 3 4 5', '15', 'EASY', 1000, 131072, 2, 1, 0, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM tb_problem WHERE title='[签到] N 个整数求和');

INSERT INTO tb_problem (title, description, input_description, output_description, sample_input, sample_output, difficulty, time_limit_ms, memory_limit_kb, author_id, is_visible, accepted_count, submit_count, created_at, updated_at)
SELECT '[签到] 统计元音', '输入一个仅由英文字母组成的单词，统计其中元音字母 a、e、i、o、u 的数量，忽略大小写。', '一行一个不含空格的英文单词。', '输出元音字母的数量。', 'Education', '5', 'EASY', 1000, 131072, 2, 1, 0, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM tb_problem WHERE title='[签到] 统计元音');

