var CHANNEL_ACCESS_TOKEN = 'Y7TzwnvY7ceB3vmR4qI+LBYVdJImEhqOtGN4U9zeWTWUYyzka1vdV47sEm1CHNMLt+KgJwQBUAjnDVe5T7Arh1+UB2OoUuz9on805DwhLLbOxqIXAKI67iIaaktO+xGLJj3oAHxh+vsa/VQAJzo4yQdB04t89/1O/w1cDnyilFU=';

function doPost(e) {
  var msg = JSON.parse(e.postData.contents);
  
  // 取得事件回覆 Token
  var replyToken = msg.events[0].replyToken;
  
  // 取得來源資訊
  var source = msg.events[0].source;
  var type = source.type;
  var userMessage = msg.events[0].message.text;

  // 如果使用者輸入 "id" 或 "ID"，才觸發回應
  if (userMessage === "id" || userMessage === "ID") {
    
    var replyText = "";

    if (type === 'group') {
      replyText = "這個群組的 ID 是：\n" + source.groupId;
    } else if (type === 'room') {
      replyText = "這個聊天室的 ID 是：\n" + source.roomId;
    } else {
      replyText = "這是你的個人 User ID：\n" + source.userId;
    }

    sendLineMessage(replyToken, replyText);
  }
}

function sendLineMessage(replyToken, replyText) {
  var url = 'https://api.line.me/v2/bot/message/reply';
  var options = {
    'method': 'post',
    'headers': {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + CHANNEL_ACCESS_TOKEN,
    },
    'payload': JSON.stringify({
      'replyToken': replyToken,
      'messages': [{
        'type': 'text',
        'text': replyText
      }]
    })
  };
  UrlFetchApp.fetch(url, options);
}

doPost("test")