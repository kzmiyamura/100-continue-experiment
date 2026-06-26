import {
  Component,
  OnInit,
  OnDestroy,
  ViewChild,
  ElementRef,
  ChangeDetectorRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { ApiService } from './api.service';

interface LogLine {
  timestamp: string;
  type: string;
  message: string;
  data?: any;
}

type RequestState = 'idle' | 'pending' | 'completed' | 'error';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule, HttpClientModule],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit, OnDestroy {
  @ViewChild('logContainer') logContainer!: ElementRef;

  // Send Request
  message = '';
  requestState: RequestState = 'idle';
  response: any = null;
  errorMessage = '';

  // Config
  delaySeconds = 10;
  configMessage = '';

  // SSE / Live Log
  sseConnected = false;
  liveLog: LogLine[] = [];

  private sseSub: Subscription | null = null;

  constructor(private api: ApiService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.loadConfig();
    this.connectSSE();
  }

  ngOnDestroy(): void {
    this.sseSub?.unsubscribe();
    this.api.disconnectSSE();
  }

  loadConfig(): void {
    this.api.getConfig().subscribe({
      next: (cfg) => {
        this.delaySeconds = cfg.delaySeconds;
        this.cdr.detectChanges();
      },
      error: () => {}
    });
  }

  sendRequest(): void {
    if (!this.message.trim()) return;
    this.requestState = 'pending';
    this.response = null;
    this.errorMessage = '';

    this.api.sendData(this.message).subscribe({
      next: (res) => {
        this.response = res;
        this.requestState = 'completed';
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.errorMessage = err.error?.errorMessage || err.message || '接続エラーが発生しました';
        this.requestState = 'error';
        this.cdr.detectChanges();
      }
    });
  }

  setConfig(): void {
    this.api.setConfig(this.delaySeconds).subscribe({
      next: (res) => {
        this.configMessage = `設定完了: ${res.delaySeconds}秒`;
        setTimeout(() => { this.configMessage = ''; this.cdr.detectChanges(); }, 3000);
        this.cdr.detectChanges();
      },
      error: () => {
        this.configMessage = '設定に失敗しました';
        this.cdr.detectChanges();
      }
    });
  }

  connectSSE(): void {
    this.sseSub = this.api.connectSSE().subscribe({
      next: (event) => {
        const now = new Date().toLocaleTimeString('ja-JP');

        if (event.type === 'connected') {
          this.sseConnected = true;
          this.addLog(now, 'connected', 'SSE接続しました');
        } else if (event.type === 'header_received') {
          this.addLog(now, 'header', `リクエスト受信: ${event.data.remoteAddr} (ID: ${event.data.id?.slice(0, 8)}...)`, event.data);
        } else if (event.type === 'update') {
          const status = event.data.status;
          if (status === 'COMPLETED') {
            this.addLog(now, 'completed', `ボディ受信完了 (ID: ${event.data.id?.slice(0, 8)}...): "${event.data.body}"`, event.data);
            // Also update config display if delay changed
          } else if (status === 'ERROR') {
            this.addLog(now, 'error', `エラー (ID: ${event.data.id?.slice(0, 8)}...): ${event.data.errorMessage}`, event.data);
          } else if (status === 'TIMEOUT') {
            this.addLog(now, 'timeout', `タイムアウト (ID: ${event.data.id?.slice(0, 8)}...)`, event.data);
          }
        } else if (event.type === 'error') {
          this.sseConnected = false;
          this.addLog(now, 'error', 'SSE接続エラー');
          // Try to reconnect after 5s
          setTimeout(() => this.connectSSE(), 5000);
        }

        this.cdr.detectChanges();
        this.scrollLogToBottom();
      }
    });
  }

  private addLog(timestamp: string, type: string, message: string, data?: any): void {
    this.liveLog.push({ timestamp, type, message, data });
    if (this.liveLog.length > 100) {
      this.liveLog.shift();
    }
  }

  private scrollLogToBottom(): void {
    setTimeout(() => {
      if (this.logContainer?.nativeElement) {
        const el = this.logContainer.nativeElement;
        el.scrollTop = el.scrollHeight;
      }
    }, 50);
  }

  formatJson(obj: any): string {
    return JSON.stringify(obj, null, 2);
  }

  getLogClass(type: string): string {
    switch (type) {
      case 'connected': return 'log-connected';
      case 'header': return 'log-header';
      case 'completed': return 'log-completed';
      case 'error': return 'log-error';
      case 'timeout': return 'log-timeout';
      default: return '';
    }
  }
}
