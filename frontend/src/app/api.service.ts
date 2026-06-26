import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { environment } from '../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private readonly baseUrl = environment.apiUrl;
  private eventSource: EventSource | null = null;
  private sseSubject = new Subject<{ type: string; data: any }>();

  constructor(private http: HttpClient) {}

  sendData(message: string): Observable<any> {
    return this.http.post(`${this.baseUrl}/api/data`, { message });
  }

  postTestBody(body: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/api/test-body`, body);
  }

  getLogs(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/api/logs`);
  }

  setConfig(delaySeconds: number): Observable<any> {
    return this.http.post(`${this.baseUrl}/api/config`, { delaySeconds });
  }

  getConfig(): Observable<any> {
    return this.http.get(`${this.baseUrl}/api/config`);
  }

  connectSSE(): Observable<{ type: string; data: any }> {
    this.disconnectSSE();

    const url = `${this.baseUrl}/api/events`;
    this.eventSource = new EventSource(url);

    this.eventSource.addEventListener('connected', (event: MessageEvent) => {
      this.sseSubject.next({ type: 'connected', data: JSON.parse(event.data) });
    });

    this.eventSource.addEventListener('header_received', (event: MessageEvent) => {
      this.sseSubject.next({ type: 'header_received', data: JSON.parse(event.data) });
    });

    this.eventSource.addEventListener('update', (event: MessageEvent) => {
      this.sseSubject.next({ type: 'update', data: JSON.parse(event.data) });
    });

    this.eventSource.onerror = () => {
      this.sseSubject.next({ type: 'error', data: { message: 'SSE connection error' } });
    };

    return this.sseSubject.asObservable();
  }

  disconnectSSE(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
  }

  isSSEConnected(): boolean {
    return this.eventSource !== null && this.eventSource.readyState === EventSource.OPEN;
  }
}
