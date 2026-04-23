import Navbar from "@/components/Navbar";

export default function ProtectedLayout({ children }) {
  return (
    <div>
      <Navbar />
      {children}
    </div>
  );
}