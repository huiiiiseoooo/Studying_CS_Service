import type { ComponentProps } from 'react'
import Markdown, { type ExtraProps } from 'react-markdown'
import remarkGfm from 'remark-gfm'

/** 코드 블록 위에 보여 줄 언어 이름. 목록에 없으면 마크다운에 적힌 이름을 그대로 쓴다. */
const LANGUAGE_LABELS: Record<string, string> = {
  java: 'Java',
  python: 'Python',
  c: 'C',
  cpp: 'C++',
  javascript: 'JavaScript',
  typescript: 'TypeScript',
  sql: 'SQL',
}

/** ```java 처럼 언어를 적은 코드 블록에 언어 이름 머리표를 붙인다. */
function CodeBlock({ node, children, ...rest }: ComponentProps<'pre'> & ExtraProps) {
  const code = node?.children[0]
  const classes = code?.type === 'element' ? code.properties.className : undefined
  const language = Array.isArray(classes)
    ? classes.map(String).find((c) => c.startsWith('language-'))?.slice('language-'.length)
    : undefined

  if (!language) return <pre {...rest}>{children}</pre>
  return (
    <div className="code-block">
      <div className="code-block__lang">{LANGUAGE_LABELS[language] ?? language}</div>
      <pre {...rest}>{children}</pre>
    </div>
  )
}

/** 학습 설명(마크다운). HTML은 해석하지 않으므로 DB 내용이 그대로 스크립트로 실행될 일은 없다. */
export function Explanation({ markdown }: { markdown: string }) {
  return (
    <div className="prose">
      <Markdown remarkPlugins={[remarkGfm]} components={{ pre: CodeBlock }}>
        {markdown}
      </Markdown>
    </div>
  )
}
