// N8N「Code_解析AI結果」節點 — 完整替換用
const prev = $('Code_組裝Prompt').first().json;
const ai = $input.first().json;

function extractAiText(payload) {
  if (!payload) return '';
  if (typeof payload === 'string') return payload;
  if (payload.choices?.[0]?.message?.content) return payload.choices[0].message.content;
  if (Array.isArray(payload)) {
    const first = payload[0];
    if (first?.content?.parts?.[0]?.text) return first.content.parts[0].text;
    return extractAiText(first);
  }
  if (payload.content?.parts?.[0]?.text) return payload.content.parts[0].text;
  if (payload.body) return extractAiText(payload.body);
  if (payload.data) return extractAiText(payload.data);
  return '';
}

let content = extractAiText(ai) || JSON.stringify(ai);
content = content.trim()
  .replace(/^```json\s*/i, '')
  .replace(/^```\s*/i, '')
  .replace(/```\s*$/i, '');

let parsed;
try {
  parsed = JSON.parse(content);
} catch (e) {
  parsed = { summary: 'AI 回覆解析失敗', suggestions: content, fullReview: content, severity: 3 };
}

function suggestionsToText(suggestions) {
  if (!suggestions) return '';
  if (typeof suggestions === 'string') return suggestions;
  if (Array.isArray(suggestions)) {
    return suggestions
      .filter(Boolean)
      .map((s) => (String(s).trim().startsWith('- ') ? String(s).trim() : `- ${String(s).trim()}`))
      .join('\n\n');
  }
  return String(suggestions);
}

const suggestionsText = suggestionsToText(parsed.suggestions);

return [{
  json: {
    reviewId: prev.reviewId,
    callbackUrl: prev.callbackUrl,
    status: 'COMPLETED',
    summary: parsed.summary || '',
    suggestions: suggestionsText,
    fullReview: parsed.fullReview || suggestionsText || content,
    severity: Number(parsed.severity) || 3,
  },
}];
