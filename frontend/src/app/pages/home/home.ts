import { Component } from '@angular/core';
import { LucideArrowRight, LucidePackage, LucidePlus } from '@lucide/angular';
import { Header } from '../../components/header/header';
import { Sidebar } from '../../components/sidebar/sidebar';
import { ServiceSummary } from '../../components/service-summary/service-summary';
import { TablesOverview } from '../../components/tables-overview/tables-overview';
import { RecentOrders } from '../../components/recent-orders/recent-orders';
import { OrdersTable } from '../../components/orders-table/orders-table';
import { Footer } from '../../components/footer/footer';

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
export class Home {}
