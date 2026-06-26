import { 
  format, 
  addMonths, 
  subMonths, 
  startOfMonth, 
  endOfMonth, 
  startOfWeek, 
  endOfWeek, 
  isSameMonth, 
  isSameDay, 
  addDays, 
  eachDayOfInterval 
} from 'date-fns';
import { ptBR } from 'date-fns/locale';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { useState } from 'react';

export function CalendarView({ 
  onSelectDate, 
  selectedDate,
  appointments = []
}: { 
  onSelectDate: (date: Date) => void,
  selectedDate: Date,
  appointments?: any[]
}) {
  const [currentMonth, setCurrentMonth] = useState(new Date());

  const prevMonth = () => setCurrentMonth(subMonths(currentMonth, 1));
  const nextMonth = () => setCurrentMonth(addMonths(currentMonth, 1));

  const monthStart = startOfMonth(currentMonth);
  const monthEnd = endOfMonth(monthStart);
  const startDate = startOfWeek(monthStart);
  const endDate = endOfWeek(monthEnd);

  const calendarDays = eachDayOfInterval({
    start: startDate,
    end: endDate
  });

  const dayLabels = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'];

  return (
    <div className="bg-white rounded-3xl shadow-sm border border-gray-100 overflow-hidden">
      <header className="p-6 flex items-center justify-between border-b border-gray-50">
        <h3 className="text-xl font-bold text-gray-900 capitalize">
          {format(currentMonth, 'MMMM yyyy', { locale: ptBR })}
        </h3>
        <div className="flex gap-2">
          <button onClick={prevMonth} className="p-2 hover:bg-gray-50 rounded-full transition-colors">
            <ChevronLeft size={20} />
          </button>
          <button onClick={nextMonth} className="p-2 hover:bg-gray-50 rounded-full transition-colors">
            <ChevronRight size={20} />
          </button>
        </div>
      </header>

      <div className="p-4">
        <div className="grid grid-cols-7 mb-2">
          {dayLabels.map(label => (
            <div key={label} className="text-center text-xs font-bold text-gray-400 py-2">
              {label}
            </div>
          ))}
        </div>

        <div className="grid grid-cols-7 gap-1">
          {calendarDays.map((day, idx) => {
            const isToday = isSameDay(day, new Date());
            const isSelected = isSameDay(day, selectedDate);
            const isOtherMonth = !isSameMonth(day, monthStart);
            const hasAppointments = appointments.some(app => isSameDay(new Date(app.date), day));

            return (
              <button
                key={day.toString()}
                onClick={() => onSelectDate(day)}
                className={`
                  aspect-square rounded-xl flex flex-col items-center justify-center relative transition-all group
                  ${isOtherMonth ? 'text-gray-300' : 'text-gray-700'}
                  ${isSelected ? 'bg-emerald-600 text-white shadow-md scale-105 z-10' : 'hover:bg-emerald-50'}
                  ${isToday && !isSelected ? 'text-emerald-600 font-bold' : ''}
                `}
              >
                <span className="text-sm">{format(day, 'd')}</span>
                {hasAppointments && !isSelected && (
                  <div className={`w-1.5 h-1.5 rounded-full mt-1 ${isOtherMonth ? 'bg-gray-200' : 'bg-emerald-400'}`} />
                )}
                {isToday && (
                  <div className={`absolute bottom-1 w-1 h-1 rounded-full ${isSelected ? 'bg-white' : 'bg-emerald-600'}`} />
                )}
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}
