import { createContext, useContext, useState, ReactNode, useEffect } from "react";

export interface StudySession {
  id: string;
  friendId: string;
  startTime: number;
  endTime: number;
  duration: number; // in seconds
  examId?: string;
}

export interface Friend {
  id: string;
  name: string;
  color: string;
}

export interface Exam {
  id: string;
  name: string;
  date: string;
  isActive: boolean;
}

interface StudyContextType {
  sessions: StudySession[];
  friends: Friend[];
  exams: Exam[];
  currentUserId: string;
  addSession: (session: StudySession) => void;
  addFriend: (friend: Friend) => void;
  addExam: (exam: Exam) => void;
  updateExam: (id: string, updates: Partial<Exam>) => void;
  deleteExam: (id: string) => void;
  setCurrentUser: (id: string) => void;
}

const StudyContext = createContext<StudyContextType | undefined>(undefined);

const STORAGE_KEY = "study-app-data";

export function StudyProvider({ children }: { children: ReactNode }) {
  const [sessions, setSessions] = useState<StudySession[]>([]);
  const [friends, setFriends] = useState<Friend[]>([]);
  const [exams, setExams] = useState<Exam[]>([]);
  const [currentUserId, setCurrentUserId] = useState<string>("");

  // Load data from localStorage on mount
  useEffect(() => {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      try {
        const data = JSON.parse(stored);
        setSessions(data.sessions || []);
        setFriends(data.friends || []);
        setExams(data.exams || []);
        setCurrentUserId(data.currentUserId || "");
      } catch (error) {
        console.error("Failed to parse stored data", error);
      }
    }

    // Initialize with current user if none exists
    if (!localStorage.getItem(STORAGE_KEY)) {
      const defaultFriend: Friend = {
        id: "user-1",
        name: "Me",
        color: "#3b82f6",
      };
      setFriends([defaultFriend]);
      setCurrentUserId(defaultFriend.id);
    }
  }, []);

  // Save to localStorage whenever data changes
  useEffect(() => {
    const data = {
      sessions,
      friends,
      exams,
      currentUserId,
    };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
  }, [sessions, friends, exams, currentUserId]);

  const addSession = (session: StudySession) => {
    setSessions((prev) => [...prev, session]);
  };

  const addFriend = (friend: Friend) => {
    setFriends((prev) => [...prev, friend]);
  };

  const addExam = (exam: Exam) => {
    setExams((prev) => [...prev, exam]);
  };

  const updateExam = (id: string, updates: Partial<Exam>) => {
    setExams((prev) =>
      prev.map((exam) => (exam.id === id ? { ...exam, ...updates } : exam))
    );
  };

  const deleteExam = (id: string) => {
    setExams((prev) => prev.filter((exam) => exam.id !== id));
  };

  const setCurrentUser = (id: string) => {
    setCurrentUserId(id);
  };

  return (
    <StudyContext.Provider
      value={{
        sessions,
        friends,
        exams,
        currentUserId,
        addSession,
        addFriend,
        addExam,
        updateExam,
        deleteExam,
        setCurrentUser,
      }}
    >
      {children}
    </StudyContext.Provider>
  );
}

export function useStudy() {
  const context = useContext(StudyContext);
  if (!context) {
    throw new Error("useStudy must be used within StudyProvider");
  }
  return context;
}
