// SHA256-based hash generator for blockchain verification
// Generates standard 66-char hex format: 0x + 64 lowercase hex digits

function hexEncode(buffer) {
  return Array.from(new Uint8Array(buffer))
    .map(b => b.toString(16).padStart(2, '0'))
    .join('')
}

export async function generateHash(content) {
  const encoder = new TextEncoder()
  const data = encoder.encode(content + Date.now().toString() + Math.random().toString())
  const hashBuffer = await crypto.subtle.digest('SHA-256', data)
  return '0x' + hexEncode(hashBuffer)
}

export async function generateDocumentHash(type, name, extra) {
  const content = [type, name, extra || '', Date.now().toString(), Math.random().toString()].join('|')
  const encoder = new TextEncoder()
  const hashBuffer = await crypto.subtle.digest('SHA-256', encoder.encode(content))
  return '0x' + hexEncode(hashBuffer)
}

export function isValidHash(hash) {
  if (!hash || typeof hash !== 'string') return false
  return /^0x[0-9a-fA-F]{64}$/.test(hash)
}

export function formatHash(hash) {
  if (!hash || hash.length < 10) return hash
  return hash.slice(0, 10) + '...' + hash.slice(-8)
}