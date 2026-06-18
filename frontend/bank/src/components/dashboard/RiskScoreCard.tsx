import { Card, CardContent, CardHeader, CardTitle } from '../common/Card';
import { ShieldAlert } from 'lucide-react';

interface RiskScoreCardProps {
  score?: number;
}

export function RiskScoreCard({ score = 0 }: RiskScoreCardProps) {
  const label = score >= 85 ? 'Critical Risk Level' : score >= 65 ? 'High Risk Level' : score >= 35 ? 'Medium Risk Level' : 'Low Risk Level';
  const color = score >= 85 ? 'text-red-600' : score >= 65 ? 'text-orange-600' : score >= 35 ? 'text-amber-600' : 'text-emerald-600';

  return (
    <Card>
      <CardHeader>
        <CardTitle>Average Risk Score</CardTitle>
      </CardHeader>
      <CardContent className="p-6 flex flex-col items-center justify-center h-48">
        <div className="relative w-32 h-32 flex items-center justify-center">
          <svg className="w-full h-full transform -rotate-90" viewBox="0 0 36 36">
            <path
              className="text-slate-100"
              strokeWidth="3"
              stroke="currentColor"
              fill="none"
              d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
            />
            <path
              className={color}
              strokeDasharray={`${score}, 100`}
              strokeWidth="3"
              strokeLinecap="round"
              stroke="currentColor"
              fill="none"
              d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
            />
          </svg>
          <div className="absolute flex flex-col items-center">
            <span className="text-3xl font-bold text-slate-900">{score}</span>
            <span className="text-xs text-slate-500">/ 100</span>
          </div>
        </div>
        <p className={`mt-4 text-sm font-medium ${color} flex items-center gap-1`}>
          <ShieldAlert className="w-4 h-4" /> {label}
        </p>
      </CardContent>
    </Card>
  );
}
