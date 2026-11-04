import React, { Component, ErrorInfo, ReactNode } from 'react';
import { AlertTriangle, RefreshCw, Home } from 'lucide-react';

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
    error: null
  };

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('ErrorBoundary caught an unhandled error:', error, errorInfo);
  }

  private handleReset = () => {
    this.setState({ hasError: false, error: null });
    try {
      localStorage.setItem('lm_current_view', 'search');
    } catch {
      // ignore
    }
    window.location.reload();
  };

  private handleGoSearch = () => {
    this.setState({ hasError: false, error: null });
    try {
      localStorage.setItem('lm_current_view', 'search');
    } catch {
      // ignore
    }
    window.location.href = window.location.pathname;
  };

  public render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }

      return (
        <div className="min-h-screen bg-gray-100 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-xl border border-gray-200 max-w-md w-full p-6 text-center">
            <div className="w-14 h-14 rounded-2xl bg-red-100 text-red-600 flex items-center justify-center mx-auto mb-4">
              <AlertTriangle className="w-7 h-7" />
            </div>
            <h2 className="text-lg font-bold text-gray-900 mb-2">
              Wystąpił problem z wyświetleniem widoku
            </h2>
            <p className="text-xs text-gray-600 mb-4 leading-relaxed">
              Aplikacja napotkała nieoczekiwany błąd. Nie martw się – Twoje dane w magazynie są bezpieczne.
            </p>
            {this.state.error?.message && (
              <div className="bg-gray-50 border border-gray-200 rounded-xl p-2.5 mb-4 text-[11px] font-mono text-gray-600 break-all text-left max-h-24 overflow-y-auto">
                {this.state.error.message}
              </div>
            )}
            <div className="flex gap-2">
              <button
                onClick={this.handleGoSearch}
                className="flex-1 py-2.5 px-3 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs flex items-center justify-center gap-1.5 transition-colors cursor-pointer"
              >
                <Home className="w-4 h-4" />
                Wyszukiwarka
              </button>
              <button
                onClick={this.handleReset}
                className="flex-1 py-2.5 px-3 rounded-xl bg-gray-200 hover:bg-gray-300 text-gray-800 font-bold text-xs flex items-center justify-center gap-1.5 transition-colors cursor-pointer"
              >
                <RefreshCw className="w-4 h-4" />
                Odśwież stronę
              </button>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
