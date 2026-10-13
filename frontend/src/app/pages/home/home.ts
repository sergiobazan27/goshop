import { Component, inject, OnInit } from '@angular/core';
import { LucideArrowRight, LucidePackage, LucidePlus } from '@lucide/angular';
import { Header } from '../../components/header/header';
import { Sidebar } from '../../components/sidebar/sidebar';
import { ServiceSummary } from '../../components/service-summary/service-summary';
import { TablesOverview } from '../../components/tables-overview/tables-overview';
import { RecentOrders } from '../../components/recent-orders/recent-orders';
import { OrdersTable } from '../../components/orders-table/orders-table';
import { Footer } from '../../components/footer/footer';
import { HttpClient } from '@angular/common/http';

@Component({
  imports: [
    Header,
    Sidebar,
    ServiceSummary,
    TablesOverview,
    RecentOrders,
    OrdersTable,
    Footer,
    LucidePlus,
    LucideArrowRight,
    LucidePackage,
  ],
  selector: 'app-home',
  styleUrl: './home.css',
  templateUrl: './home.html',
})
export class Home implements OnInit {
  private readonly http = inject(HttpClient);

  ngOnInit(): void {
    this.http
      .get<{
        sub: string;
        username: string;
        roles: string[];
      }>('http://localhost:8080/api/me')
      .subscribe({
        next: (user) => console.info('Usuario autenticado: ', user),
        error: (error) => console.error('Error consultado /api/me: ', error),
      });
  }
}
