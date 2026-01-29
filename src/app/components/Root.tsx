import { Outlet, Link, useLocation } from "react-router";
import { Timer as TimerIcon, History as HistoryIcon, Trophy, Users } from "lucide-react";
import { StudyProvider } from "@/app/context/StudyContext";

export function Root() {
  const location = useLocation();

  const isActive = (path: string) => {
    if (path === "/") {
      return location.pathname === "/";
    }
    return location.pathname.startsWith(path);
  };

  return (
    <StudyProvider>
      <div className="min-h-screen bg-gradient-to-br from-indigo-50 to-blue-50 flex flex-col pb-20">
        {/* Header */}
        <header className="bg-white shadow-sm">
          <div className="max-w-7xl mx-auto px-4 py-3">
            <h1 className="text-xl font-bold text-gray-900">StudyBuddy</h1>
            <p className="text-xs text-gray-600">Track your study time with friends</p>
          </div>
        </header>

        {/* Main Content */}
        <main className="flex-1 w-full px-4 py-4">
          <Outlet />
        </main>

        {/* Bottom Navigation */}
        <nav className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 shadow-lg">
          <div className="grid grid-cols-4">
            <Link
              to="/"
              className={`flex flex-col items-center py-3 px-2 transition-colors ${
                isActive("/")
                  ? "text-indigo-600"
                  : "text-gray-600 hover:text-indigo-500"
              }`}
            >
              <TimerIcon className="w-6 h-6 mb-1" />
              <span className="text-xs font-medium">Timer</span>
            </Link>

            <Link
              to="/history"
              className={`flex flex-col items-center py-3 px-2 transition-colors ${
                isActive("/history")
                  ? "text-indigo-600"
                  : "text-gray-600 hover:text-indigo-500"
              }`}
            >
              <HistoryIcon className="w-6 h-6 mb-1" />
              <span className="text-xs font-medium">History</span>
            </Link>

            <Link
              to="/exams"
              className={`flex flex-col items-center py-3 px-2 transition-colors ${
                isActive("/exams")
                  ? "text-indigo-600"
                  : "text-gray-600 hover:text-indigo-500"
              }`}
            >
              <Trophy className="w-6 h-6 mb-1" />
              <span className="text-xs font-medium">Exams</span>
            </Link>

            <Link
              to="/friends"
              className={`flex flex-col items-center py-3 px-2 transition-colors ${
                isActive("/friends")
                  ? "text-indigo-600"
                  : "text-gray-600 hover:text-indigo-500"
              }`}
            >
              <Users className="w-6 h-6 mb-1" />
              <span className="text-xs font-medium">Friends</span>
            </Link>
          </div>
        </nav>
      </div>
    </StudyProvider>
  );
}