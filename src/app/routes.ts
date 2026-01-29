import { createBrowserRouter } from "react-router";
import { Root } from "@/app/components/Root";
import { Timer } from "@/app/pages/Timer";
import { History } from "@/app/pages/History";
import { Exams } from "@/app/pages/Exams";
import { Friends } from "@/app/pages/Friends";

export const router = createBrowserRouter([
  {
    path: "/",
    Component: Root,
    children: [
      { index: true, Component: Timer },
      { path: "history", Component: History },
      { path: "exams", Component: Exams },
      { path: "friends", Component: Friends },
    ],
  },
]);
