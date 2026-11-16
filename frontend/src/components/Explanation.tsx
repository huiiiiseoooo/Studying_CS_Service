import Markdown from 'react-markdown'
import remarkGfm from 'remark-gfm'

/** 학습 설명(마크다운). HTML은 해석하지 않으므로 DB 내용이 그대로 스크립트로 실행될 일은 없다. */
export function Explanation({ markdown }: { markdown: string }) {
  return (
    <div className="prose">
      <Markdown remarkPlugins={[remarkGfm]}>{markdown}</Markdown>
    </div>
  )
}
