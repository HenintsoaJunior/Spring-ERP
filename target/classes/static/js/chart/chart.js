document.addEventListener('DOMContentLoaded', function() {
    // Get data from the window object that was populated in the inline script
    const chartData = window.dashboardData.chart1Data;
    const chart2DataJson = window.dashboardData.chart2DataJson;
    const chart3DataJson = window.dashboardData.chart3DataJson;
    const selectedYear = window.dashboardData.selectedYear;
    const selectedMonth = window.dashboardData.selectedMonth;
    
    // Chart 1: Statistiques des Clients
    if (chartData) {
        // Préparation des données pour le graphique
        const labels = chartData.map(data => data.company_name);
        const totalInvoices = chartData.map(data => Math.floor(data.total_invoices)); // Forcer en entiers
        const totalInvoiced = chartData.map(data => data.total_invoiced_amount || 0);
        const outstanding = chartData.map(data => data.outstanding_amount || 0);
        const externalIds = chartData.map(data => data.external_id);
    
        // Graphique principal (mainChart)
        const ctx = document.getElementById('mainChart').getContext('2d');
        const mainChart = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Nombre de factures',
                    data: totalInvoices,
                    backgroundColor: 'rgba(54, 162, 235, 0.5)',
                    borderColor: 'rgba(54, 162, 235, 1)',
                    borderWidth: 1
                }, {
                    label: 'Montant facturé',
                    data: totalInvoiced,
                    backgroundColor: 'rgba(75, 192, 192, 0.5)',
                    borderColor: 'rgba(75, 192, 192, 1)',
                    borderWidth: 1
                }, {
                    label: 'Montant restant',
                    data: outstanding,
                    backgroundColor: 'rgba(255, 99, 132, 0.5)',
                    borderColor: 'rgba(255, 99, 132, 1)',
                    borderWidth: 1
                }]
            },
            options: {
                responsive: true,
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            stepSize: 1, // Pas de 1 pour les entiers
                            precision: 0 // Pas de décimales
                        }
                    }
                },
                plugins: {
                    legend: {
                        position: 'top',
                    },
                    title: {
                        display: true,
                        text: 'Statistiques des Clients'
                    },
                    tooltip: {
                        callbacks: {
                            label: function(context) {
                                let label = context.dataset.label || '';
                                if (label) {
                                    label += ': ';
                                }
                                // Forcer l'affichage en entier pour "Nombre de factures"
                                if (context.dataset.label === 'Nombre de factures') {
                                    label += Math.round(context.parsed.y);
                                } else {
                                    label += context.parsed.y;
                                }
                                return label;
                            }
                        }
                    }
                },
                onClick: (event, elements) => {
                    if (elements.length > 0) {
                        const index = elements[0].index;
                        const externalId = externalIds[index];
                        window.location.href = `/api/spring/client/${externalId}`;
                    }
                }
            }
        });
    
        // Graphique secondaire (invoiceChart)
        const ctxInvoices = document.getElementById('invoiceChart').getContext('2d');
        const invoiceChart = new Chart(ctxInvoices, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Nombre de factures',
                    data: totalInvoices,
                    backgroundColor: 'rgba(54, 162, 235, 0.5)',
                    borderColor: 'rgba(54, 162, 235, 1)',
                    borderWidth: 1
                }]
            },
            options: {
                indexAxis: 'y', // Barres horizontales
                responsive: true,
                maintainAspectRatio: true,
                scales: {
                    x: {
                        beginAtZero: true,
                        ticks: {
                            stepSize: 1, // Pas de 1 pour les entiers
                            precision: 0 // Pas de décimales
                        },
                        title: {
                            display: true,
                            text: 'Nombre de factures'
                        }
                    }
                },
                plugins: {
                    legend: {
                        position: 'top',
                    },
                    tooltip: {
                        callbacks: {
                            label: function(context) {
                                let label = context.dataset.label || '';
                                if (label) {
                                    label += ': ';
                                }
                                label += Math.round(context.parsed.x); // Forcer en entier
                                return label;
                            }
                        }
                    }
                },
                onClick: (event, elements) => {
                    if (elements.length > 0) {
                        const index = elements[0].index;
                        const externalId = externalIds[index];
                        window.location.href = `/api/spring/client/${externalId}`;
                    }
                }
            }
        });
    }
    // Chart 2: Statistiques des Paiements
    // let paymentChartData;

    // try {
    //     paymentChartData = JSON.parse(chart2DataJson);
    // } catch (e) {
    //     paymentChartData = [];
    //     console.error('Error parsing chart2DataJson:', e);
    // }

    // if (paymentChartData && Array.isArray(paymentChartData) && paymentChartData.length > 0) {
    //     const monthNames = ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Juin', 
    //                     'Juil', 'Août', 'Sep', 'Oct', 'Nov', 'Déc'];
        
    //     // Create labels for each month
    //     const labels = paymentChartData.map(item => {
    //         const [year, month] = item.payment_month.split('-');
    //         const monthName = monthNames[parseInt(month) - 1];
    //         return `${monthName} ${year}`;
    //     });

    //     const paidAmounts = paymentChartData.map(item => parseFloat(item.total_paid_amount) || 0);
    //     const invoicedAmounts = paymentChartData.map(item => parseFloat(item.total_invoiced_amount) || 0);
    //     const outstandingAmounts = paymentChartData.map(item => parseFloat(item.outstanding_amount) || 0);

    //     const ctx = document.getElementById('paymentChart').getContext('2d');
    //     const chart = new Chart(ctx, {
    //         type: 'line',  // Changed from 'bar' to 'line'
    //         data: {
    //             labels: labels,
    //             datasets: [{
    //                 label: 'Montant payé',
    //                 data: paidAmounts,
    //                 backgroundColor: 'rgba(54, 162, 235, 0.6)',
    //                 borderColor: 'rgba(54, 162, 235, 1)',
    //                 borderWidth: 1,
    //                 fill: false
    //             }, {
    //                 label: 'Montant facturé',
    //                 data: invoicedAmounts,
    //                 backgroundColor: 'rgba(75, 192, 192, 0.6)',
    //                 borderColor: 'rgba(75, 192, 192, 1)',
    //                 borderWidth: 1,
    //                 fill: false
    //             }, {
    //                 label: 'Montant restant',
    //                 data: outstandingAmounts,
    //                 backgroundColor: 'rgba(255, 99, 132, 0.6)',
    //                 borderColor: 'rgba(255, 99, 132, 1)',
    //                 borderWidth: 1,
    //                 fill: false
    //             }]
    //         },
    //         options: {
    //             responsive: true,
    //             scales: {
    //                 y: {
    //                     beginAtZero: true,
    //                     title: { display: true, text: 'Montant' }
    //                 },
    //                 x: {
    //                     title: { display: true, text: 'Mois' },
    //                     ticks: {
    //                         autoSkip: false,
    //                         maxRotation: 45,
    //                         minRotation: 45
    //                     }
    //                 }
    //             },
    //             plugins: {
    //                 legend: { position: 'top' },
    //                 title: {
    //                     display: true,
    //                     text: `Statistiques des paiements pour ${selectedYear}` + 
    //                             (selectedMonth ? ` - ${monthNames[parseInt(selectedMonth) - 1]}` : '')
    //                 },
    //                 tooltip: {
    //                     callbacks: {
    //                         label: function(context) {
    //                             const label = context.dataset.label || '';
    //                             const value = context.parsed.y || 0;
    //                             return `${label}: ${value.toLocaleString()}`;
    //                         }
    //                     }
    //                 }
    //             }
    //         }
    //     });
    // }

    let paymentChartData;

    try {
        paymentChartData = JSON.parse(chart2DataJson);
    } catch (e) {
        paymentChartData = [];
        console.error('Error parsing chart2DataJson:', e);
    }

    if (paymentChartData && Array.isArray(paymentChartData) && paymentChartData.length > 0) {
        const monthNames = ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Juin', 
                        'Juil', 'Août', 'Sep', 'Oct', 'Nov', 'Déc'];
        
        // Create labels for each month
        const labels = paymentChartData.map(item => {
            const [year, month] = item.payment_month.split('-');
            const monthName = monthNames[parseInt(month) - 1];
            return `${monthName} ${year}`;
        });

        const paidAmounts = paymentChartData.map(item => parseFloat(item.total_paid_amount) || 0);
        const invoicedAmounts = paymentChartData.map(item => parseFloat(item.total_invoiced_amount) || 0);
        const outstandingAmounts = paymentChartData.map(item => parseFloat(item.outstanding_amount) || 0);

        const ctx = document.getElementById('paymentChart').getContext('2d');
        const chart = new Chart(ctx, {
            type: 'bar',  // Changed from 'line' to 'bar'
            data: {
                labels: labels,
                datasets: [{
                    label: 'Montant payé',
                    data: paidAmounts,
                    backgroundColor: 'rgba(54, 162, 235, 0.6)',
                    borderColor: 'rgba(54, 162, 235, 1)',
                    borderWidth: 1
                }, {
                    label: 'Montant facturé',
                    data: invoicedAmounts,
                    backgroundColor: 'rgba(75, 192, 192, 0.6)',
                    borderColor: 'rgba(75, 192, 192, 1)',
                    borderWidth: 1
                }, {
                    label: 'Montant restant',
                    data: outstandingAmounts,
                    backgroundColor: 'rgba(255, 99, 132, 0.6)',
                    borderColor: 'rgba(255, 99, 132, 1)',
                    borderWidth: 1
                }]
            },
            options: {
                responsive: true,
                scales: {
                    y: {
                        beginAtZero: true,
                        title: { display: true, text: 'Montant' }
                    },
                    x: {
                        title: { display: true, text: 'Mois' },
                        ticks: {
                            autoSkip: false,
                            maxRotation: 45,
                            minRotation: 45
                        }
                    }
                },
                plugins: {
                    legend: { position: 'top' },
                    title: {
                        display: true,
                        text: `Statistiques des paiements pour ${selectedYear}` + 
                                (selectedMonth ? ` - ${monthNames[parseInt(selectedMonth) - 1]}` : '')
                    },
                    tooltip: {
                        callbacks: {
                            label: function(context) {
                                const label = context.dataset.label || '';
                                const value = context.parsed.y || 0;
                                return `${label}: ${value.toLocaleString()}`;
                            }
                        }
                    }
                }
            }
        });
    }
    
    // Chart 3: Statistiques des Factures
    let invoiceChart3; // Variable globale pour le graphique pie

    // Parsing des données JSON
    let invoiceChartData;
    try {
        invoiceChartData = JSON.parse(chart3DataJson);
    } catch (e) {
        invoiceChartData = [];
        console.error('Error parsing chart3DataJson:', e);
    }

    // Vérification initiale des données
    console.log('Données brutes chart3DataJson:', invoiceChartData);

    if (invoiceChartData && Array.isArray(invoiceChartData) && invoiceChartData.length > 0) {
        // Préparation des données pour le graphique Pie
        const labels = invoiceChartData.map(item => item.invoice_status);
        const invoicedAmounts = invoiceChartData.map(item => parseFloat(item.total_invoiced_amount) || 0);

        // Couleurs pastel modernes avec distinction claire
        const backgroundColors = [
            '#FF0000',
            '#FF9999', // Rouge pastel (ex: unpaid)
            '#66CC99', // Vert menthe (ex: paid)
            '#FFCC66', // Orange clair (ex: partially paid)
            '#99CCFF', // Bleu pastel (ex: pending)
            '#CC99FF', // Violet pastel (ex: overdue)
            '#FFFF99'  // Jaune pastel (ex: draft)
        ];
        const hoverColors = backgroundColors.map(color => {
            return color.replace('99', 'B3').replace('66', '80');
        });

        // Détruisez le graphique existant s'il existe
        if (invoiceChart3) {
            invoiceChart3.destroy();
        }

        // Graphique Pie
        const ctx = document.getElementById('invoiceChart3').getContext('2d');
        invoiceChart3 = new Chart(ctx, {
            type: 'pie', // Changé de 'doughnut' à 'pie'
            data: {
                labels: labels,
                datasets: [{
                    label: 'Montant facturé',
                    data: invoicedAmounts,
                    backgroundColor: backgroundColors.slice(0, labels.length),
                    hoverBackgroundColor: hoverColors.slice(0, labels.length),
                    borderWidth: 1, // Bordure fine pour un pie chart
                    borderColor: '#fff' // Bordure blanche pour séparer les segments
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                animation: {
                    animateScale: true,
                    animateRotate: true,
                    duration: 1000,
                    easing: 'easeInOutQuart'
                },
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: {
                            font: { size: 14, family: "'Roboto', sans-serif" },
                            padding: 15,
                            boxWidth: 20,
                            usePointStyle: true,
                            color: '#555'
                        }
                    },
                    title: {
                        display: true,
                        text: 'Répartition des montants facturés par statut',
                        font: { size: 12, family: "'Roboto', sans-serif", weight: '500' },
                        padding: { top: 20, bottom: 10 },
                        color: '#333'
                    },
                    tooltip: {
                        backgroundColor: '#fff',
                        titleColor: '#333',
                        bodyColor: '#666',
                        borderColor: '#ddd',
                        borderWidth: 1,
                        cornerRadius: 8,
                        padding: 12,
                        callbacks: {
                            label: function(context) {
                                const label = context.label || '';
                                const value = context.parsed || 0;
                                const total = context.dataset.data.reduce((a, b) => a + b, 0);
                                const percentage = ((value / total) * 100).toFixed(1);
                                return `${label}: ${value.toLocaleString()} (${percentage}%)`;
                            }
                        }
                    },
                    datalabels: {
                        color: '#333',
                        font: { size: 12, family: "'Roboto', sans-serif", weight: 'bold' },
                        formatter: (value, context) => {
                            const total = context.dataset.data.reduce((a, b) => a + b, 0);
                            const percentage = ((value / total) * 100).toFixed(1);
                            return percentage > 5 ? `${percentage}%` : '';
                        },
                        anchor: 'center',
                        align: 'center'
                    }
                }
            },
            plugins: [ChartDataLabels] // Nécessaire pour les étiquettes de données
        });
    } else {
        console.log('Aucune donnée valide pour Chart 3');
    }
});