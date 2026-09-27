/**
 * PII Data Masking Utilities (NFR-SEC-004)
 * Masks customer Personally Identifiable Information to prevent unauthorized exposure
 * in administrative interfaces.
 */

export function maskEmail(email?: string | null): string {
  if (!email) {
    return '';
  }
  if (!email.includes('@')) {
    return '***';
  }

  const [localPart, domain] = email.split('@');
  if (localPart.length <= 2) {
    return `${localPart.slice(0, 1)}***@${domain}`;
  }

  return `${localPart.slice(0, 2)}***@${domain}`;
}

export function maskPhone(phone?: string | null): string {
  if (!phone) {
    return '';
  }

  const cleaned = phone.trim();
  if (cleaned.length <= 4) {
    return '****';
  }

  if (cleaned.length <= 7) {
    return `${cleaned.slice(0, 2)}****`;
  }

  // Format: 09****5678 (2 head digits + **** + 4 tail digits)
  return `${cleaned.slice(0, 2)}****${cleaned.slice(-4)}`;
}
