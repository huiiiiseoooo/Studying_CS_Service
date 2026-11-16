import { useParams } from 'react-router'

/** URL의 분류 ID를 숫자로 읽는다. 숫자가 아니면 null. */
export function useIdParam(name: string): number | null {
  const value = useParams()[name]
  return value && /^\d{1,15}$/.test(value) ? Number(value) : null
}
