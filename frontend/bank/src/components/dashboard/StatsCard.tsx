import type { LucideIcon } from 'lucide-react';
import { Card, CardContent } from '../common/Card';

interface StatsCardProps {
  title: string;
  value: string | number;
  icon: LucideIcon;
  trend: number;
  trendLabel: string;
  colorClass: string;
}

export function StatsCard({ title, value, icon: Icon, trend, trendLabel, colorClass }: StatsCardProps) {
  return (
    <Card>
      <CardContent className="p-6">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-slate-500 mb-1">{title}</p>
            <h4 className="text-2xl font-bold text-slate-900">{value}</h4>
          </div>
          <div className={`p-3 rounded-xl ${colorClass}`}>
            <Icon className="w-6 h-6" />
          </div>
        </div>
        <div className="mt-4 flex items-center text-sm">
          <span className={trend > 0 ? "text-emerald-600 font-medium" : "text-red-600 font-medium"}>
            {trend > 0 ? '+' : ''}{trend}%
          </span>
          <span className="text-slate-500 ml-2">{trendLabel}</span>
        </div>
      </CardContent>
    </Card>
  );
}
