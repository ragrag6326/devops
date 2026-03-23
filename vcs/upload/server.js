const express = require('express');
const multer = require('multer');
const { NodeSSH } = require('node-ssh');
const dayjs = require('dayjs');
const path = require('path');
const fs = require('fs');

const app = express();
const ssh = new NodeSSH();
const upload = multer({ dest: 'temp/' });

// --- ssh 配置區 ---
const SSH_CONFIG = {
    host: '132.145.125.250',
    //host: '192.168.1.35',
    username: 'tkb0001662',
    //username: 'tkbuser',
    privateKey: fs.readFileSync('/opt/upload/prod.pem').toString(), // 私鑰路徑
};

const REMOTE_DEST_DIR = "/opt/docker/upload/upload/pending/";
const COUNTER_FILE = './counter.json';

const BASE_URL = "https://www.tkbtv.com.tw/upload/pending/";
// --------------

// 取得並更新編號的函式
function getNextSequence() {
    let data = { lastDate: '', count: 0 };
    const today = dayjs().format('YYYYMMDD');

    if (fs.existsSync(COUNTER_FILE)) {
        data = JSON.parse(fs.readFileSync(COUNTER_FILE));
    }

    // 如果日期變了，編號重置為 1；否則累加
    if (data.lastDate !== today) {
        data.lastDate = today;
        data.count = 1;
    } else {
        data.count += 1;
    }

    fs.writeFileSync(COUNTER_FILE, JSON.stringify(data));
    return { dateStr: today, seq: String(data.count).padStart(5, '0') };
}

app.post('/api/upload', upload.array('files'), async (req, res) => {
    const files = req.files;
    const results = [];
    const dynamicPath = req.body.destination || "/opt/docker/upload/upload/pending/";

    try {
        // 連接 SSH
        console.log("接收到 /api/upload 請求");

        
        await ssh.connect(SSH_CONFIG);

        for (const file of files) {
            const { dateStr, seq } = getNextSequence();
            const ext = path.extname(file.originalname);
            const newFileName = `${dateStr}${seq}${ext}`;
            const localPath = file.path;
            const remotePath = path.join(dynamicPath, newFileName);

            // 執行上傳
            await ssh.putFile(localPath, remotePath);
            
            // 刪除本地暫存
            fs.unlinkSync(localPath);
            
            // 修改這裡：回傳完整的 URL 鏈結
            results.push({ 
                original: file.originalname, 
                uploaded: newFileName,
                url: `${BASE_URL}${newFileName}` // 產生完整連結
            });
        }

        res.json({ success: true, files: results });
    } catch (err) {
        console.error('Deployment Error:', err);
        res.status(500).json({ success: false, message: err.message });
    } finally {
        ssh.dispose(); // 斷開連線
    }
});

app.listen(3000, () => console.log('Server running on http://localhost:3000'));