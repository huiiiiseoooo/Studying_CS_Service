import { Navigate, Route, Routes } from 'react-router'
import { AdminLayout } from './components/AdminLayout.tsx'
import { SiteLayout } from './components/SiteLayout.tsx'
import { AdminLoginPage } from './pages/AdminLoginPage.tsx'
import { AdminStatsPage } from './pages/AdminStatsPage.tsx'
import { LearnPage } from './pages/LearnPage.tsx'
import { MajorPage } from './pages/MajorPage.tsx'
import { MediumPage } from './pages/MediumPage.tsx'
import { MinorPage } from './pages/MinorPage.tsx'
import { NotFoundPage } from './pages/NotFoundPage.tsx'

/**
 * 분류 위치를 URL에 그대로 담는다. (TC-001, QC-008)
 *   /                                   대분류 목록
 *   /categories/:majorId                중분류 목록
 *   /categories/:majorId/:mediumId      소분류 목록
 *   /categories/:majorId/:mediumId/:minorId   학습 화면
 */
export default function App() {
  return (
    <Routes>
      <Route element={<SiteLayout />}>
        <Route index element={<MajorPage />} />
        <Route path="categories/:majorId" element={<MediumPage />} />
        <Route path="categories/:majorId/:mediumId" element={<MinorPage />} />
        <Route path="categories/:majorId/:mediumId/:minorId" element={<LearnPage />} />
        <Route path="admin/login" element={<AdminLoginPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
      <Route path="admin" element={<AdminLayout />}>
        <Route index element={<Navigate to="stats" replace />} />
        <Route path="stats" element={<AdminStatsPage />} />
      </Route>
    </Routes>
  )
}
