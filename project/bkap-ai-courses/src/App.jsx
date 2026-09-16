import React, { Suspense, lazy } from "react";
import {
  BrowserRouter,
  Routes,
  Route,
  Navigate,
  useLocation,
} from "react-router-dom";
import { ToastContainer } from "react-toastify";
import { AnimatePresence } from "framer-motion";
import Navbar from "./components/layout/Navbar";
import Footer from "./components/layout/Footer";
import Skeleton from "./components/ui/Skeleton";
import ScrollToTop from "./components/ui/ScrollToTop";
import RouteScrollToTop from "./components/layout/RouteScrollToTop";
import PageTransition from "./components/layout/PageTransition";
import { AuthProvider } from "./context/AuthContext";
import AuthModal from "./components/ui/AuthModal";
import ProtectedRoute from "./components/ProtectedRoute";

// Toastify Styles
import "react-toastify/dist/ReactToastify.css";
import CheckoutPage from "./pages/checkout/CheckoutPage";
import PaymentResultPage from "./pages/checkout/PaymentResultPage";

// Lazy-load Pages for Code Splitting and Performance Optimization
const Home = lazy(() => import("./pages/Home"));
const Courses = lazy(() => import("./pages/Courses"));
const CourseDetail = lazy(() => import("./pages/CourseDetail"));
const About = lazy(() => import("./pages/About"));
const Contact = lazy(() => import("./pages/Contact"));
const NotFound = lazy(() => import("./pages/NotFound"));
const LearnPage = lazy(() => import("./pages/LearnPage"));
const QuizLamBaiPage = lazy(() => import("./pages/quiz/QuizLamBaiPage"));
const QuizKetQuaPage = lazy(() => import("./pages/quiz/QuizKetQuaPage"));

// Dashboard Pages (lazy-load)
const DashboardLayout = lazy(() => import("./pages/dashboard/DashboardLayout"));
const DashboardHome = lazy(() => import("./pages/dashboard/DashboardHome"));
const MyCoursesPage = lazy(() => import("./pages/dashboard/MyCoursesPage"));
const ProfilePage = lazy(() => import("./pages/dashboard/ProfilePage"));
const SettingsPage = lazy(() => import("./pages/dashboard/SettingsPage"));

// Visual fallback page loader skeleton
function PageSkeleton() {
  return (
    <div className="max-w-7xl mx-auto px-4 md:px-6 lg:px-8 py-20 min-h-[60vh] flex flex-col justify-center">
      <div className="h-8 bg-gray-200 rounded-md w-1/4 mb-6 animate-pulse" />
      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
        {[...Array(3)].map((_, i) => (
          <Skeleton key={i} />
        ))}
      </div>
    </div>
  );
}

function AnimatedRoutes() {
  const location = useLocation();

  return (
    <AnimatePresence mode="wait">
      <Routes location={location} key={location.pathname}>
        <Route
          path="/"
          element={
            <PageTransition>
              <Home />
            </PageTransition>
          }
        />
        <Route
          path="/courses"
          element={
            <PageTransition>
              <Courses />
            </PageTransition>
          }
        />
        <Route
          path="/courses/:id"
          element={
            <PageTransition>
              <CourseDetail />
            </PageTransition>
          }
        />
        <Route
          path="/about"
          element={
            <PageTransition>
              <About />
            </PageTransition>
          }
        />
        <Route
          path="/contact"
          element={
            <PageTransition>
              <Contact />
            </PageTransition>
          }
        />
        <Route
          path="/not-found"
          element={
            <PageTransition>
              <NotFound />
            </PageTransition>
          }
        />

        {/* Trang học: nội dung bài giảng lấy từ LMS, bắt buộc đăng nhập */}
        <Route
          path="/hoc/:courseId"
          element={
            <ProtectedRoute>
              <PageTransition>
                <LearnPage />
              </PageTransition>
            </ProtectedRoute>
          }
        />

        {/* Làm bài kiểm tra ngay trên buni — điểm do LMS chấm */}
        <Route
          path="/hoc/:courseId/kiem-tra/:cmid/lam/:attemptId"
          element={
            <ProtectedRoute>
              <QuizLamBaiPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/hoc/:courseId/kiem-tra/:cmid/ket-qua/:attemptId"
          element={
            <ProtectedRoute>
              <PageTransition>
                <QuizKetQuaPage />
              </PageTransition>
            </ProtectedRoute>
          }
        />

        <Route path="/checkout/:courseId" element={<CheckoutPage />} />
        <Route path="/payment/result" element={<PaymentResultPage />} />

        {/* Fallback redirect for other invalid path URLs */}
        <Route path="*" element={<Navigate to="/not-found" replace />} />
      </Routes>
    </AnimatePresence>
  );
}

/**
 * Dashboard Routes — tách riêng, không có Navbar/Footer thông thường
 * Được bảo vệ bởi ProtectedRoute
 */
function DashboardRoutes() {
  return (
    <ProtectedRoute>
      <Suspense
        fallback={
          <div className="flex items-center justify-center min-h-screen">
            <div className="w-8 h-8 border-4 border-red-600 border-t-transparent rounded-full animate-spin" />
          </div>
        }
      >
        <DashboardLayout>
          <Routes>
            <Route index element={<DashboardHome />} />
            <Route path="my-courses" element={<MyCoursesPage />} />
            <Route path="profile" element={<ProfilePage />} />
            <Route path="settings" element={<SettingsPage />} />
          </Routes>
        </DashboardLayout>
      </Suspense>
    </ProtectedRoute>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <RouteScrollToTop />

        {/*
        Routing tổng: phân chia route /dashboard (không có Navbar/Footer)
        và các route thông thường (có Navbar/Footer)
      */}
        <Routes>
          {/* Dashboard — layout riêng, protected */}
          <Route path="/dashboard/*" element={<DashboardRoutes />} />

          {/* Public pages — có Navbar + Footer */}
          <Route
            path="/*"
            element={
              <div className="flex flex-col min-h-screen">
                <Navbar />
                <div className="flex-grow">
                  <Suspense fallback={<PageSkeleton />}>
                    <AnimatedRoutes />
                  </Suspense>
                </div>
                <Footer />
              </div>
            }
          />
        </Routes>

        {/* Global alert toast notifications */}
        <ToastContainer
          position="top-right"
          autoClose={3000}
          hideProgressBar={false}
          newestOnTop={false}
          closeOnClick
          rtl={false}
          pauseOnFocusLoss
          draggable
          pauseOnHover
          theme="colored"
        />
        <ScrollToTop />
        <AuthModal />
      </BrowserRouter>
    </AuthProvider>
  );
}
