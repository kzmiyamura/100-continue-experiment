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

  // Body Format Test
  testIdValue = '';
  testResult: { label: string; status: string; data: any } | null = null;
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

  async testBody(type: string): Promise<void> {
    this.testResult = null;
    const url = '/api/test-body';
    const headers = { 'Content-Type': 'application/json' };
    const idVal = this.testIdValue || 'value';

    const labels: Record<string, string> = {
      'normal':           `① 正常送信 → id:"${idVal}"`,
      'null':             '② null値  → id:null',
      'empty-string':     '③ 空文字列 → id:""',
      'empty-body':       '④ 空ボディ → bodyなし',
      'truncated':        '⑤ 途中で切れた → 先頭7byteのみ',
      'ref-null-before':  '⑥ subscribe前にnull代入',
      'ref-null-mutate':  '⑦ subscribe前にプロパティをnullに変更',
    };

    let body: BodyInit | undefined;

    if (type === 'ref-null-before') {
      // HttpClientにオブジェクトを渡してからsubscribe前にnullを代入
      let obj: any = { id: idVal };
      const obs = this.api.postTestBody(obj);
      obj = null; // 参照を差し替え（HttpClientが持つ参照には影響しない）
      obs.subscribe({
        next: (data: any) => {
          this.testResult = { label: labels[type] + `（結果: obj=nullにしたが元の値が送られるか？）`, status: 'result-ok', data };
          this.cdr.detectChanges();
        },
        error: (e: any) => {
          this.testResult = { label: labels[type], status: 'result-error', data: { error: e.message } };
          this.cdr.detectChanges();
        }
      });
      return;
    }

    if (type === 'ref-null-mutate') {
      // HttpClientにオブジェクトを渡してからsubscribe前にプロパティをnullに変更
      const obj: any = { id: idVal };
      const obs = this.api.postTestBody(obj);
      obj.id = null; // プロパティを変更（同じ参照なのでHttpClientにも影響する？）
      obs.subscribe({
        next: (data: any) => {
          this.testResult = { label: labels[type] + `（結果: id=nullにしたが反映されるか？）`, status: 'result-ok', data };
          this.cdr.detectChanges();
        },
        error: (e: any) => {
          this.testResult = { label: labels[type], status: 'result-error', data: { error: e.message } };
          this.cdr.detectChanges();
        }
      });
      return;
    }

    switch (type) {
      case 'normal':       body = JSON.stringify({ id: idVal }); break;
      case 'null':         body = JSON.stringify({ id: null }); break;
      case 'empty-string': body = JSON.stringify({ id: '' }); break;
      case 'empty-body':   body = undefined; break;
      case 'truncated':
        const full = JSON.stringify({ id: idVal });
        body = new Blob([full.slice(0, 7)], { type: 'application/json' });
        break;
    }

    try {
      const res = await fetch(url, { method: 'POST', headers, body });
      const data = await res.json();
      this.testResult = {
        label: labels[type],
        status: res.ok ? 'result-ok' : 'result-error',
        data
      };
    } catch (e: any) {
      this.testResult = {
        label: labels[type],
        status: 'result-error',
        data: { error: e.message }
      };
    }
    this.cdr.detectChanges();
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
