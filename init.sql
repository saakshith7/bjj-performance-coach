CREATE TABLE IF NOT EXISTS users (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(100) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  belt VARCHAR(20) DEFAULT 'white',
  weight_kg DECIMAL(5,2),
  age INT,
  training_days_per_week INT DEFAULT 3,
  fitness_level VARCHAR(20) DEFAULT 'beginner',
  goal VARCHAR(50) DEFAULT 'fitness',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sessions (
  id VARCHAR(36) PRIMARY KEY,
  user_id VARCHAR(36),
  session_date DATE NOT NULL,
  duration_minutes INT,
  session_type VARCHAR(20) DEFAULT 'gi',
  energy_level INT,
  notes TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS position_logs (
  id VARCHAR(36) PRIMARY KEY,
  session_id VARCHAR(36),
  position VARCHAR(50) NOT NULL,
  outcome VARCHAR(10),
  role VARCHAR(10),
  FOREIGN KEY (session_id) REFERENCES sessions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS technique_logs (
  id VARCHAR(36) PRIMARY KEY,
  session_id VARCHAR(36),
  technique VARCHAR(100) NOT NULL,
  category VARCHAR(50),
  success BOOLEAN DEFAULT false,
  notes TEXT,
  FOREIGN KEY (session_id) REFERENCES sessions(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS weakness_reports (
  id VARCHAR(36) PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  total_sessions_analyzed INT,
  weak_positions JSON,
  weak_techniques JSON,
  role_analysis JSON,
  cardio_flag BOOLEAN DEFAULT false,
  recommendations JSON,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS sc_programs (
  id VARCHAR(36) PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  duration_weeks INT DEFAULT 8,
  fitness_level VARCHAR(20),
  target_weaknesses JSON,
  weekly_schedule JSON,
  notes TEXT,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);